import { useState } from "react";
import {
  MapContainer,
  TileLayer,
  GeoJSON
} from "react-leaflet";
import "leaflet/dist/leaflet.css";
import "./App.css";

const API = "http://localhost:8080";

function App() {
  const [file, setFile] = useState(null);
  const [documentId, setDocumentId] = useState(null);
  const [results, setResults] = useState([]);
  const [summary, setSummary] = useState(null);
  const [parcel, setParcel] = useState(null);
  const [records, setRecords] = useState([]);
  const [dashboardStats, setDashboardStats] = useState({
  total: 0,
  active: 0,
  pendingVerification: 0,
  verified: 0,
  rejected: 0
});

  const [uploading, setUploading] = useState(false);
  const [processing, setProcessing] = useState(false);
  const [validating, setValidating] = useState(false);
  const [verifying, setVerifying] = useState(false);

  const [message, setMessage] = useState("");
  const [previewUrl, setPreviewUrl] = useState(null);

  const loadRecords = async () => {
  try {
    const recordsResponse = await fetch(
      `${API}/api/land-records`
    );

    if (!recordsResponse.ok) {
      throw new Error("Failed to load land records");
    }

    const recordsData = await recordsResponse.json();

    setRecords(recordsData);

    const statsResponse = await fetch(
      `${API}/api/documents/stats`
    );

    if (!statsResponse.ok) {
      throw new Error("Failed to load document stats");
    }

    const stats = await statsResponse.json();

    console.log("LAND RECORDS:", recordsData);
    console.log("DOCUMENT STATS:", stats);

    setDashboardStats({
      total: recordsData.length,

      active: recordsData.filter(
        (record) => record.status === "ACTIVE"
      ).length,

      pendingVerification: stats.pendingVerification || 0,

      verified: stats.verified || 0,

      rejected: stats.rejected || 0
    });

  } catch (error) {
    console.error("Dashboard error:", error);
  }
};

  

  const handleFileChange = (event) => {
    const selectedFile = event.target.files[0];

    if (!selectedFile) {
      return;
    }

    setFile(selectedFile);
    setPreviewUrl(
      URL.createObjectURL(selectedFile)
    );

    setDocumentId(null);
    setResults([]);
    setSummary(null);
    setParcel(null);
    setMessage("");
  };

  const handleUpload = async () => {
    if (!file) {
      setMessage(
        "Please select a land document."
      );
      return;
    }

    setUploading(true);
    setMessage("");

    const formData = new FormData();

    formData.append("file", file);

    try {
      const response = await fetch(
        `${API}/api/documents/upload`,
        {
          method: "POST",
          body: formData
        }
      );

      const data = await response.json();

      if (!response.ok) {
        throw new Error(
          data.error || "Upload failed"
        );
      }

      setDocumentId(data.documentId);

      setMessage(
        `Document uploaded successfully. ID: ${data.documentId}`
      );

    } catch (error) {
      setMessage(error.message);
    } finally {
      setUploading(false);
    }
  };

  const handleProcess = async () => {
    if (!documentId) {
      return;
    }

    setProcessing(true);
    setMessage("");

    try {
      const response = await fetch(
        `${API}/api/documents/${documentId}/process`,
        {
          method: "POST"
        }
      );

      const data = await response.json();

      if (!response.ok) {
        throw new Error(
          data.error || "AI processing failed"
        );
      }

      setMessage(
        "AI OCR and field extraction completed successfully."
      );

    } catch (error) {
      setMessage(error.message);
    } finally {
      setProcessing(false);
    }
  };

  const handleValidation = async () => {
    if (!documentId) {
      return;
    }

    setValidating(true);
    setMessage("");
    setParcel(null);

    try {
      const response = await fetch(
        `${API}/api/validation/${documentId}`,
        {
          method: "POST"
        }
      );

      const data = await response.json();

      if (!response.ok) {
        throw new Error(
          data.error || "Validation failed"
        );
      }

      setResults(data);

      const surveyResult = data.find(
        (item) =>
          item.fieldName === "survey_number"
      );

      if (surveyResult?.extractedValue) {

        const parcelResponse = await fetch(
          `${API}/api/parcels?surveyNumber=${encodeURIComponent(
            surveyResult.extractedValue
          )}`
        );

        if (parcelResponse.ok) {

          const parcelData =
            await parcelResponse.json();

          setParcel({
            ...parcelData,
            geometry: JSON.parse(
              parcelData.geometry
            )
          });
        }
      }

      const summaryResponse = await fetch(
        `${API}/api/validation/${documentId}/summary`
      );

      const summaryData =
        await summaryResponse.json();

      if (!summaryResponse.ok) {
        throw new Error(
          summaryData.error ||
          "Failed to load validation summary"
        );
      }

      setSummary(summaryData);

      setMessage(
        "Validation completed successfully."
      );

    } catch (error) {
      setMessage(error.message);
    } finally {
      setValidating(false);
    }
  };

  const handleVerification = async (action) => {
    if (!documentId) {
      return;
    }

    setVerifying(true);
    setMessage("");

    try {
      const response = await fetch(
        `${API}/api/verification/${documentId}`,
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json"
          },
          body: JSON.stringify({
            reviewerId: 1,
            action: action,
            comments:
              action === "APPROVE"
                ? "Document reviewed and approved."
                : "Document reviewed and rejected."
          })
        }
      );

      const data = await response.json();

      if (!response.ok) {
        throw new Error(
          data.error || "Verification failed"
        );
      }

      setSummary((previous) => ({
        ...previous,
        verificationStatus:
          action === "APPROVE"
            ? "VERIFIED"
            : "REJECTED"
      }));

      setMessage(
        action === "APPROVE"
          ? "Document approved successfully."
          : "Document rejected successfully."
      );

    } catch (error) {
      setMessage(error.message);
    } finally {
      setVerifying(false);
    }
  };

  return (
    <div className="app">

      <header className="header">

        <div className="brand">
  <img
    src="/khetrify-logo.jpeg"
    alt="Kshetrify Logo"
    className="brand-logo"
  />

  <div>
    <h1>KSHETRIFY</h1>
    <p>Intelligent Land Record Digitization</p>
  </div>
</div>

        <div className="status">
          <span></span>
          System Online
        </div>

      </header>

      <main className="container">

        <section className="dashboard-section">

          <div className="section-header">

            <div>
              <h2>
                Land Records Dashboard
              </h2>

              <p className="subtitle">
                Overview of registered land records
              </p>
            </div>

            <button
  className="dashboard-refresh"
  onClick={loadRecords}
>
  Load Records
</button>

          </div>

         <div className="dashboard-cards">

  <div className="dashboard-card">
    <span>Total Land Records</span>
    <strong>{dashboardStats.total}</strong>
  </div>

  <div className="dashboard-card active-card">
    <span>Active Records</span>
    <strong>{dashboardStats.active}</strong>
  </div>

  <div className="dashboard-card pending-card">
    <span>Pending Verification</span>
    <strong>{dashboardStats.pendingVerification}</strong>
  </div>

  <div className="dashboard-card verified-card">
    <span>Verified Documents</span>
    <strong>{dashboardStats.verified}</strong>
  </div>

  <div className="dashboard-card">
    <span>Rejected Documents</span>
    <strong>{dashboardStats.rejected}</strong>
  </div>

</div>

          <div className="records-table">

            <div className="records-row records-header">

              <span>
                Record ID
              </span>

              <span>
                Owner
              </span>

              <span>
                Survey Number
              </span>

              <span>
                Village
              </span>

              <span>
                Area
              </span>

              <span>
                Status
              </span>

            </div>

            {records.map((record) => (

              <div
                className="records-row"
                key={record.id}
              >

                <span>
                  {record.recordId}
                </span>

                <span>
                  {record.ownerName}
                </span>

                <span>
                  {record.surveyNumber}
                </span>

                <span>
                  {record.village}
                </span>

                <span>
                  {record.area}{" "}
                  {record.areaUnit}
                </span>

                <span className="record-status">
                  {record.status}
                </span>

              </div>

            ))}

          </div>

        </section>

        <section className="upload-section">

          <h2>
            Land Document Processing
          </h2>

          <p className="subtitle">
            Upload a land document and let Kshetrify
            automatically identify and validate the
            corresponding master record.
          </p>

          <div className="document-layout">

            <div
              className="upload-box"
              onClick={() =>
                document
                  .getElementById("fileInput")
                  .click()
              }
            >

              <div className="upload-icon">
                ↑
              </div>

              <h3>
                {file
                  ? file.name
                  : "Choose a land document"}
              </h3>

              <p>
                {file
                  ? "Document selected successfully"
                  : "PNG, JPG or PDF up to 10 MB"}
              </p>

              <input
                id="fileInput"
                type="file"
                accept=".png,.jpg,.jpeg,.pdf"
                onChange={handleFileChange}
              />

            </div>

            {previewUrl &&
              file?.type.startsWith("image/") && (

                <div className="preview">

                  <h3>
                    Document Preview
                  </h3>

                  <img
                    src={previewUrl}
                    alt="Uploaded land document"
                  />

                </div>

              )}

          </div>

          <div className="pipeline">

            <div
              className={
                file
                  ? "step active"
                  : "step"
              }
            >
              <span>1</span>
              Upload
            </div>

            <div
              className={
                processing ||
                results.length > 0
                  ? "step active"
                  : "step"
              }
            >
              <span>2</span>
              AI OCR
            </div>

            <div
              className={
                summary
                  ? "step active"
                  : "step"
              }
            >
              <span>3</span>
              Validation
            </div>

            <div
              className={
                summary?.verificationStatus ===
                "VERIFIED"
                  ? "step active"
                  : "step"
              }
            >
              <span>4</span>
              Verification
            </div>

          </div>

          <button
            className="process-btn"
            disabled={
              !file || uploading
            }
            onClick={handleUpload}
          >
            {uploading
              ? "Uploading..."
              : "Upload Document"}
          </button>

          {documentId && (
            <>
              <button
                className="process-btn"
                onClick={handleProcess}
                disabled={processing}
              >
                {processing
                  ? "Running AI OCR..."
                  : "Process with AI"}
              </button>

              <button
                className="process-btn"
                onClick={handleValidation}
                disabled={validating}
              >
                {validating
                  ? "Validating..."
                  : "Validate Record"}
              </button>
            </>
          )}

          {message && (
            <div className="message">
              {message}
            </div>
          )}

        </section>

        {summary && (

          <section className="results-section">

            <div className="section-header">

              <div>

                <h2>
                  Validation Results
                </h2>

                <p className="subtitle">
                  AI-extracted fields compared with
                  the automatically identified master record.
                </p>

              </div>

              <div className="risk-badge">
                {summary.overallRisk} RISK
              </div>

            </div>

            <div className="summary">

              <div className="summary-card">

                <span>
                  Total Fields
                </span>

                <strong>
                  {summary.totalFields}
                </strong>

              </div>

              <div className="summary-card match">

                <span>
                  Matched
                </span>

                <strong>
                  {summary.matchedFields}
                </strong>

              </div>

              <div className="summary-card mismatch">

                <span>
                  Mismatched
                </span>

                <strong>
                  {summary.mismatchedFields}
                </strong>

              </div>

            </div>

            <div className="table">

              <div className="table-row table-header">

                <span>
                  Field
                </span>

                <span>
                  Extracted
                </span>

                <span>
                  Master Record
                </span>

                <span>
                  Status
                </span>

              </div>

              {results.map((result) => (

                <div
                  className="table-row"
                  key={result.id}
                >

                  <span>
                    {result.fieldName}
                  </span>

                  <span>
                    {result.extractedValue}
                  </span>

                  <span>
                    {result.referenceValue}
                  </span>

                  <span
                    className={
                      result.validationStatus ===
                      "MATCH"
                        ? "match-text"
                        : "mismatch-text"
                    }
                  >
                    {result.validationStatus}
                  </span>

                </div>

              ))}

            </div>

            <div className="verification-box">

              <div>

                <h3>

                  {summary.verificationStatus ===
                  "HUMAN_VERIFICATION_REQUIRED"
                    ? "Human Verification Required"
                    : summary.verificationStatus ===
                      "VERIFIED"
                    ? "✓ Document Verified"
                    : summary.verificationStatus ===
                      "REJECTED"
                    ? "✕ Document Rejected"
                    : "✓ Verification Not Required"}

                </h3>

                <p>

                  {summary.verificationStatus ===
                  "HUMAN_VERIFICATION_REQUIRED"
                    ? "Significant discrepancies were detected. An authorized reviewer must verify this document."
                    : summary.verificationStatus ===
                      "VERIFIED"
                    ? "The document has been reviewed and approved by an authorized reviewer."
                    : summary.verificationStatus ===
                      "REJECTED"
                    ? "The document has been reviewed and rejected by an authorized reviewer."
                    : "All extracted fields match the master record. Human verification is not required."}

                </p>

              </div>

              {summary.verificationStatus ===
                "HUMAN_VERIFICATION_REQUIRED" && (

                <div className="actions">

                  <button
                    className="reject-btn"
                    disabled={verifying}
                    onClick={() =>
                      handleVerification(
                        "REJECT"
                      )
                    }
                  >
                    {verifying
                      ? "Processing..."
                      : "Reject"}
                  </button>

                  <button
                    className="approve-btn"
                    disabled={verifying}
                    onClick={() =>
                      handleVerification(
                        "APPROVE"
                      )
                    }
                  >
                    {verifying
                      ? "Processing..."
                      : "Approve"}
                  </button>

                </div>

              )}

            </div>

          </section>

        )}

        {parcel && (

          <section className="map-section">

            <h2>
              GIS Parcel Verification
            </h2>

            <p className="subtitle">
              Survey Number:{" "}
              {parcel.surveyNumber}
            </p>

            <MapContainer
              center={[
                23.031,
                72.501
              ]}
              zoom={16}
              style={{
                height: "400px",
                width: "100%",
                marginTop: "20px",
                borderRadius: "12px"
              }}
            >

              <TileLayer
                attribution="&copy; OpenStreetMap contributors"
                url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
              />

              <GeoJSON
                data={parcel.geometry}
              />

            </MapContainer>

          </section>

        )}

      </main>

    </div>
  );
}

export default App;