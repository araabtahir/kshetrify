# Kshetrify

## Intelligent Land Record Digitization and Validation System

Kshetrify is a Smart India Hackathon 2026 prototype designed to digitize and validate land records using OCR, automated field extraction, reference-data validation, and GIS visualization.

The system converts scanned land-record documents into structured digital information and compares extracted fields with reference land records to identify matches and mismatches.

---

## 🚀 Live Prototype

🌐 **Frontend:**  
https://kshetrify.vercel.app

🔧 **Backend API:**  
https://kshetrify.onrender.com

🤖 **AI / OCR Service:**  
https://kshetrify-ai-tesseract.onrender.com

💻 **GitHub Repository:**  
https://github.com/araabtahir/kshetrify

🎥 **Project Demonstration Video:**  

https://www.youtube.com/@KSHETRIFY
---

## 🎯 Problem Statement

Land records are often available as scanned documents or semi-structured records.

Manual processing creates several challenges:

- Manual data entry takes time.
- Important fields can be missed.
- OCR can introduce spelling or numerical errors.
- Survey numbers and land areas need careful verification.
- Different records need to be compared with reference data.
- Spatial information is difficult to understand without GIS visualization.
- Manual validation can become difficult when the number of documents increases.

Kshetrify addresses these challenges through an automated digitization and validation workflow.

---

## 💡 Proposed Solution

Kshetrify provides an end-to-end workflow:

**Upload → OCR → Extract → Normalize → Validate → Risk Identification → GIS → Human Verification**

The system extracts important fields from land documents and compares them with reference records stored in the database.

---

## 🔄 System Workflow

```text
                    Land Record Document
                            │
                            ▼
                    Document Upload
                            │
                            ▼
                    Image Preprocessing
                            │
                            ▼
                         OCR
                            │
                            ▼
                    Field Extraction
                            │
                            ▼
                    Data Normalization
                            │
                            ▼
                 Reference Record Matching
                            │
                            ▼
                    Field Validation
                            │
                            ▼
                Mismatch / Risk Detection
                            │
                            ▼
                    GIS Visualization
                            │
                            ▼
                  Human Verification
                            │
                            ▼
                    Verified / Rejected


                    