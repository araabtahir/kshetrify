from fastapi import FastAPI, UploadFile, File
from paddleocr import PaddleOCR
import tempfile
import os
import json
import re

app = FastAPI(title="Kshetrify AI Service")

ocr = PaddleOCR(lang="en")


@app.get("/")
def home():
    return {
        "service": "Kshetrify AI Service",
        "status": "running"
    }


@app.get("/health")
def health():
    return {
        "status": "healthy"
    }


def classify_document(text):
    text_lower = " ".join(text).lower()

    land_terms = {
        "survey": 3,
        "survey number": 4,
        "khasra": 4,
        "khata": 3,
        "khatauni": 4,
        "mutation": 3,
        "land": 2,
        "area": 2,
        "hectare": 3,
        "acre": 3,
        "village": 3,
        "tehsil": 3,
        "district": 2,
        "plot": 2,
        "owner": 2,
        "record of rights": 5,
        "ror": 4,
        "भूमि": 3,
        "खसरा": 4,
        "खतौनी": 4,
        "तहसील": 3,
        "ग्राम": 3,
        "क्षेत्रफल": 3,
        "म्यूटेशन": 3,
        "भू-अभिलेख": 5
    }

    score = 0
    matched_terms = []

    for term, weight in land_terms.items():
        if term in text_lower:
            score += weight
            matched_terms.append(term)

    if re.search(r"\b\d+/\d+\b", text_lower):
        score += 4
        matched_terms.append("survey_pattern")

    if re.search(r"\b\d+\.\d+\b", text_lower):
        score += 2
        matched_terms.append("area_pattern")

    if re.search(r"\bmut[- ]?\d+\b", text_lower):
        score += 4
        matched_terms.append("mutation_pattern")

    if "upbhulekh.gov.in" in text_lower:
        score += 5
        matched_terms.append("land_record_portal")

    if score >= 8:
        document_type = "LAND_RECORD"
        confidence = min(0.60 + score / 30, 0.99)
    elif score >= 4:
        document_type = "UNCERTAIN"
        confidence = 0.50
    else:
        document_type = "NON_LAND_DOCUMENT"
        confidence = 0.90

    return {
        "document_type": document_type,
        "confidence": round(confidence, 2),
        "matched_terms": matched_terms
    }

def extract_fields(text):

    fields = {
        "owner_name": {"value": None, "confidence": 0.0},
        "survey_number": {"value": None, "confidence": 0.0},
        "area": {"value": None, "confidence": 0.0},
        "area_unit": {"value": None, "confidence": 0.0},
        "village": {"value": None, "confidence": 0.0},
        "tehsil": {"value": None, "confidence": 0.0},
        "district": {"value": None, "confidence": 0.0},
        "mutation_number": {"value": None, "confidence": 0.0}
    }

    lines = []

    for item in text:
        clean = " ".join(item.split()).strip()
        if clean:
            lines.append(clean)

    full_text = " ".join(lines)

    def find_label_value(labels):
        for i, line in enumerate(lines):
            for label in labels:
                match = re.search(
                    rf"^{label}\s*[:\-]?\s*(.+)$",
                    line,
                    re.IGNORECASE
                )

                if match:
                    value = match.group(1).strip()

                    if value:
                        return value

                if re.fullmatch(
                    rf"{label}\s*[:\-]?",
                    line,
                    re.IGNORECASE
                ):
                    if i + 1 < len(lines):
                        return lines[i + 1]

        return None

    owner = find_label_value([
        r"owner\s+name",
        r"owner",
        r"account\s+name"
    ])

    if owner:
        owner = re.split(
            r"\s+(?:record\s+id|father'?s\s+name|address)\b",
            owner,
            flags=re.IGNORECASE
        )[0].strip()

        fields["owner_name"] = {
            "value": owner,
            "confidence": 0.95
        }

    survey_match = re.search(
        r"(?:survey\s+(?:number|no\.?)|khasra\s+(?:number|no\.?))"
        r"\s*[:\-]?\s*(\d+/\d+)",
        full_text,
        re.IGNORECASE
    )

    if survey_match:
        fields["survey_number"] = {
            "value": survey_match.group(1),
            "confidence": 0.98
        }

    area_match = re.search(
        r"\b(\d+\.\d{1,4})\b",
        full_text
    )

    if area_match:
        fields["area"] = {
            "value": area_match.group(1),
            "confidence": 0.95
        }

    if re.search(
        r"\bhectares?\b",
        full_text,
        re.IGNORECASE
    ):
        fields["area_unit"] = {
            "value": "Hectare",
            "confidence": 0.95
        }

    village = find_label_value([
        r"village",
        r"gram"
    ])

    if village:
        village = re.split(
            r"\s+(?:tehsil|district|record\s+id)\b",
            village,
            flags=re.IGNORECASE
        )[0].strip()

        fields["village"] = {
            "value": village,
            "confidence": 0.95
        }

    tehsil = find_label_value([
        r"tehsil",
        r"tahsil"
    ])

    if tehsil:
        tehsil = re.split(
            r"\s+(?:district|village|record\s+id)\b",
            tehsil,
            flags=re.IGNORECASE
        )[0].strip()

        fields["tehsil"] = {
            "value": tehsil,
            "confidence": 0.95
        }

    district = find_label_value([
        r"district"
    ])

    if district:
        district = re.split(
            r"\s+(?:tehsil|village|record\s+id)\b",
            district,
            flags=re.IGNORECASE
        )[0].strip()

        fields["district"] = {
            "value": district,
            "confidence": 0.95
        }

    mutation_match = re.search(
        r"\b(MUT[- ]?\d+)\b",
        full_text,
        re.IGNORECASE
    )

    if mutation_match:
        fields["mutation_number"] = {
            "value": mutation_match.group(1).upper(),
            "confidence": 0.98
        }

    return fields

@app.post("/ocr")
async def perform_ocr(file: UploadFile = File(...)):

    if not file.filename:
        return {
            "error": "Filename is missing"
        }

    suffix = os.path.splitext(file.filename)[1]

    with tempfile.NamedTemporaryFile(
        delete=False,
        suffix=suffix
    ) as temp:

        contents = await file.read()
        temp.write(contents)
        temp_path = temp.name

    try:
        results = ocr.predict(temp_path)

        extracted_text = []

        for result in results:
            try:
                result_data = result.json

                if isinstance(result_data, str):
                    result_data = json.loads(result_data)

                if "res" in result_data:
                    res = result_data["res"]

                    if "rec_texts" in res:
                        extracted_text.extend(
                            res["rec_texts"]
                        )

            except Exception as error:
                print("OCR result error:", error)

        classification = classify_document(
            extracted_text
        )

        fields = extract_fields(
            extracted_text
        )

        return {
            "filename": file.filename,
            "document_type": classification["document_type"],
            "classification_confidence": classification["confidence"],
            "matched_terms": classification["matched_terms"],
            "fields": fields,
            "text": extracted_text
        }

    finally:
        if os.path.exists(temp_path):
            os.remove(temp_path)