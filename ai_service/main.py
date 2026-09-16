from fastapi import FastAPI, UploadFile, File
import tempfile
import os
import re
import cv2
import pytesseract
from PIL import Image

app = FastAPI(title="Kshetrify AI Service")


@app.get("/")
def home():
    return {
        "service": "Kshetrify AI Service",
        "status": "running",
        "ocr": "Tesseract"
    }


@app.get("/health")
def health():
    return {
        "status": "healthy"
    }


# =========================================================
# DOCUMENT CLASSIFICATION
# =========================================================

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

    if re.search(r"\b\d+\s*/\s*\d+\b", text_lower):
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


# =========================================================
# FIELD EXTRACTION
# =========================================================

def extract_fields(text):

    fields = {
        "owner_name": {
            "value": None,
            "confidence": 0.0
        },
        "survey_number": {
            "value": None,
            "confidence": 0.0
        },
        "area": {
            "value": None,
            "confidence": 0.0
        },
        "area_unit": {
            "value": None,
            "confidence": 0.0
        },
        "village": {
            "value": None,
            "confidence": 0.0
        },
        "tehsil": {
            "value": None,
            "confidence": 0.0
        },
        "district": {
            "value": None,
            "confidence": 0.0
        },
        "mutation_number": {
            "value": None,
            "confidence": 0.0
        }
    }

    # -----------------------------------------------------
    # CLEAN LINES
    # -----------------------------------------------------

    lines = []

    for item in text:
        clean = " ".join(str(item).split()).strip()

        if clean:
            lines.append(clean)

    full_text = " ".join(lines)

    # -----------------------------------------------------
    # HELPER
    # -----------------------------------------------------

    def find_label_value(labels):

        for i, line in enumerate(lines):

            for label in labels:

                # Example:
                # Village: Rampur
                # District: Ahmedabad
                match = re.search(
                    rf"\b{label}\b\s*[:\-]?\s*(.+)$",
                    line,
                    re.IGNORECASE
                )

                if match:

                    value = match.group(1).strip()

                    if value:
                        return value

                # Example:
                # Village
                # Rampur
                if re.fullmatch(
                    rf"\b{label}\b\s*[:\-]?",
                    line,
                    re.IGNORECASE
                ):

                    if i + 1 < len(lines):
                        return lines[i + 1]

        return None

    # =====================================================
    # OWNER NAME
    # =====================================================

    owner = find_label_value([
        r"owner\s+name",
        r"owner",
        r"account\s+name"
    ])

    if owner:

        owner = re.split(
            r"\s+(?:record\s+id|father'?s\s+name|address|survey|area|village|tehsil|district|mutation)\b",
            owner,
            flags=re.IGNORECASE
        )[0].strip()

        fields["owner_name"] = {
            "value": owner,
            "confidence": 0.95
        }

    # =====================================================
    # SURVEY NUMBER
    # =====================================================

    survey_match = re.search(
        r"(?:survey\s+(?:number|no\.?)|khasra\s+(?:number|no\.?))"
        r"\s*[:\-]?\s*([0-9]{1,6}\s*/\s*[0-9]{1,6})",
        full_text,
        re.IGNORECASE
    )

    if survey_match:

        survey = survey_match.group(1)

        survey = re.sub(
            r"\s+",
            "",
            survey
        )

        # OCR sometimes reads I/l as 1
        survey = survey.replace("I", "1")
        survey = survey.replace("l", "1")

        fields["survey_number"] = {
            "value": survey,
            "confidence": 0.98
        }

    # Fallback: detect standalone survey pattern
    if fields["survey_number"]["value"] is None:

        standalone_survey = re.search(
            r"\b(\d{1,6}\s*/\s*\d{1,6})\b",
            full_text
        )

        if standalone_survey:

            survey = re.sub(
                r"\s+",
                "",
                standalone_survey.group(1)
            )

            fields["survey_number"] = {
                "value": survey,
                "confidence": 0.85
            }

    # =====================================================
    # AREA
    # =====================================================

    area_match = re.search(
        r"(?:area|extent|land\s+area)"
        r"\s*(?:\([^)]*\))?"
        r"\s*[:\-]?\s*"
        r"([0-9]+(?:\.[0-9]{1,4})?)",
        full_text,
        re.IGNORECASE
    )

    if area_match:

        area = area_match.group(1)

        try:
            area = f"{float(area):.4f}"
        except Exception:
            pass

        fields["area"] = {
            "value": area,
            "confidence": 0.95
        }

    # Fallback: decimal number
    if fields["area"]["value"] is None:

        area_match = re.search(
            r"\b([0-9]+\.[0-9]{1,4})\b",
            full_text
        )

        if area_match:

            area = area_match.group(1)

            try:
                area = f"{float(area):.4f}"
            except Exception:
                pass

            fields["area"] = {
                "value": area,
                "confidence": 0.80
            }

    # =====================================================
    # AREA UNIT
    # =====================================================

    if re.search(
        r"\bhectares?\b|\bhectare\b",
        full_text,
        re.IGNORECASE
    ):

        fields["area_unit"] = {
            "value": "Hectare",
            "confidence": 0.95
        }

    elif re.search(
        r"\bacres?\b|\bacre\b",
        full_text,
        re.IGNORECASE
    ):

        fields["area_unit"] = {
            "value": "Acre",
            "confidence": 0.95
        }

    # =====================================================
    # VILLAGE
    # =====================================================

    village = find_label_value([
        r"village",
        r"gram"
    ])

    if village:

        village = re.split(
            r"\s+(?:tehsil|district|record\s+id|owner|survey|area|mutation)\b",
            village,
            flags=re.IGNORECASE
        )[0].strip()

        fields["village"] = {
            "value": village,
            "confidence": 0.95
        }

    # =====================================================
    # TEHSIL
    # =====================================================

    tehsil = find_label_value([
        r"tehsil",
        r"tahsil",
        r"tahseel"
    ])

    if tehsil:

        tehsil = re.split(
            r"\s+(?:district|village|record\s+id|owner|survey|area|mutation)\b",
            tehsil,
            flags=re.IGNORECASE
        )[0].strip()

        fields["tehsil"] = {
            "value": tehsil,
            "confidence": 0.95
        }

    # =====================================================
    # DISTRICT
    # =====================================================

    district = find_label_value([
        r"district"
    ])

    if district:

        district = re.split(
            r"\s+(?:tehsil|village|record\s+id|owner|survey|area|mutation)\b",
            district,
            flags=re.IGNORECASE
        )[0].strip()

        fields["district"] = {
            "value": district,
            "confidence": 0.95
        }

    # =====================================================
    # MUTATION NUMBER
    # =====================================================

    mutation_match = re.search(
        r"\b(MUT[- ]?[A-Za-z0-9]+)\b",
        full_text,
        re.IGNORECASE
    )

    if mutation_match:

        mutation = mutation_match.group(1).upper()

        mutation = mutation.replace(
            " ",
            ""
        )

        fields["mutation_number"] = {
            "value": mutation,
            "confidence": 0.98
        }

    # =====================================================
    # RETURN
    # =====================================================

    return fields


# =========================================================
# OCR ENDPOINT
# =========================================================

@app.post("/ocr")
async def perform_ocr(file: UploadFile = File(...)):

    if not file.filename:

        return {
            "error": "Filename is missing"
        }

    suffix = os.path.splitext(
        file.filename
    )[1].lower()

    allowed_extensions = [
        ".jpg",
        ".jpeg",
        ".png",
        ".webp"
    ]

    if suffix not in allowed_extensions:

        return {
            "error": "Unsupported file type"
        }

    temp_path = None
    processed_path = None

    try:

        # -------------------------------------------------
        # SAVE FILE
        # -------------------------------------------------

        with tempfile.NamedTemporaryFile(
            delete=False,
            suffix=suffix
        ) as temp:

            contents = await file.read()

            if not contents:

                return {
                    "error": "Uploaded file is empty"
                }

            temp.write(contents)

            temp_path = temp.name

        print(
            f"OCR request received: {file.filename}"
        )

        # -------------------------------------------------
        # READ IMAGE
        # -------------------------------------------------

        image = cv2.imread(
            temp_path
        )

        if image is None:

            return {
                "error": "Unable to read image"
            }

        # -------------------------------------------------
        # GRAYSCALE
        # -------------------------------------------------

        gray = cv2.cvtColor(
            image,
            cv2.COLOR_BGR2GRAY
        )

        # -------------------------------------------------
        # UPSCALE 2X
        # -------------------------------------------------

        gray = cv2.resize(
            gray,
            None,
            fx=2,
            fy=2,
            interpolation=cv2.INTER_CUBIC
        )

        # -------------------------------------------------
        # NOISE REDUCTION
        # -------------------------------------------------

        gray = cv2.GaussianBlur(
            gray,
            (3, 3),
            0
        )

        # -------------------------------------------------
        # ADAPTIVE THRESHOLD
        # -------------------------------------------------

        processed = cv2.adaptiveThreshold(
            gray,
            255,
            cv2.ADAPTIVE_THRESH_GAUSSIAN_C,
            cv2.THRESH_BINARY,
            31,
            11
        )

        # -------------------------------------------------
        # SAVE PROCESSED IMAGE
        # -------------------------------------------------

        processed_path = (
            temp_path + "_processed.png"
        )

        cv2.imwrite(
            processed_path,
            processed
        )

        print(
            "Starting Tesseract OCR..."
        )

        # -------------------------------------------------
        # TESSERACT OCR
        # -------------------------------------------------

        ocr_text = pytesseract.image_to_string(
            Image.open(processed_path),
            config="--oem 3 --psm 6"
        )

        print(
            "Tesseract OCR completed."
        )

        # -------------------------------------------------
        # CLEAN TEXT
        # -------------------------------------------------

        extracted_text = [
            line.strip()
            for line in ocr_text.splitlines()
            if line.strip()
        ]

        print(
            "OCR TEXT:",
            extracted_text
        )

        # -------------------------------------------------
        # CLASSIFICATION
        # -------------------------------------------------

        classification = classify_document(
            extracted_text
        )

        # -------------------------------------------------
        # EXTRACTION
        # -------------------------------------------------

        fields = extract_fields(
            extracted_text
        )

        # -------------------------------------------------
        # RESPONSE
        # -------------------------------------------------

        return {
            "filename": file.filename,
            "document_type": classification["document_type"],
            "classification_confidence": classification["confidence"],
            "matched_terms": classification["matched_terms"],
            "fields": fields,
            "text": extracted_text
        }

    except Exception as error:

        print(
            "OCR ERROR:",
            repr(error)
        )

        return {
            "error": "OCR processing failed",
            "details": str(error)
        }

    finally:

        # -------------------------------------------------
        # DELETE TEMP FILES
        # -------------------------------------------------

        if temp_path and os.path.exists(temp_path):

            try:
                os.remove(temp_path)
            except Exception:
                pass

        if processed_path and os.path.exists(processed_path):

            try:
                os.remove(processed_path)
            except Exception:
                pass