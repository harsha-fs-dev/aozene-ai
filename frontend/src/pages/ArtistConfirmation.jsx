import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import axiosClient from "../api/axiosClient";
import "./ArtistConfirmation.css";

const STATUS_INFO = {
  AWAITING_CONFIRMATION: { cls: "amber", label: "Awaiting Confirmation" },
  CONFIRMED: { cls: "sage", label: "Session Confirmed" },
  DECLINED: { cls: "terracotta", label: "Session Declined" },
  CANCELLED: { cls: "muted", label: "Session Cancelled" },
  COMPLETED: { cls: "sage", label: "Session Completed" },
};

function ArtistConfirmation() {
  const { sessionId } = useParams();
  const [session, setSession] = useState(null);
  const [loading, setLoading] = useState(true);
  const [notFound, setNotFound] = useState(false);
  const [actionLoading, setActionLoading] = useState(false);
  const [actionError, setActionError] = useState("");

  const fetchSession = async () => {
    setLoading(true);
    setNotFound(false);
    try {
      const response = await axiosClient.get(`/sessions/${sessionId}`);
      setSession(response.data);
    } catch (err) {
      setNotFound(true);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchSession();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [sessionId]);

  const handleAction = async (action) => {
    setActionLoading(true);
    setActionError("");
    try {
      const response = await axiosClient.put(`/sessions/${sessionId}/${action}`);
      setSession(response.data);
    } catch (err) {
      const backendMessage = err.response?.data?.error;
      setActionError(backendMessage || "This action could not be completed. Please refresh and try again.");
    } finally {
      setActionLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="ac-page">
        <div className="ac-card ac-card--center">
          <p className="ac-loading">Loading recording request…</p>
        </div>
      </div>
    );
  }

  if (notFound || !session) {
    return (
      <div className="ac-page">
        <div className="ac-card ac-card--center">
          <p className="ac-eyebrow">Aozene</p>
          <h1 className="ac-not-found-title">Session not found.</h1>
          <p className="ac-not-found-subtitle">
            This link may be incorrect, or the recording session no longer exists.
          </p>
        </div>
      </div>
    );
  }

  const info = STATUS_INFO[session.status] || { cls: "muted", label: session.status };
  const projectName = session.episode?.project?.name || "Unknown project";
  const episodeLabel = session.episode
    ? `${session.episode.episodeNumber ? `Episode ${session.episode.episodeNumber}` : "Episode"}${
        session.episode.title ? ` — ${session.episode.title}` : ""
      }`
    : "Unknown episode";
  const artistName = session.artist?.name || "Unknown artist";
  const isAwaiting = session.status === "AWAITING_CONFIRMATION";

  return (
    <div className="ac-page">
      <div className="ac-card">
        <p className="ac-eyebrow">Aozene</p>
        <h1 className="ac-title">Recording Confirmation</h1>

        <div className="ac-details">
          <div className="ac-detail-row">
            <span className="ac-detail-label">Artist</span>
            <span className="ac-detail-value">{artistName}</span>
          </div>
          <div className="ac-detail-row">
            <span className="ac-detail-label">Project</span>
            <span className="ac-detail-value">{projectName}</span>
          </div>
          <div className="ac-detail-row">
            <span className="ac-detail-label">Episode</span>
            <span className="ac-detail-value">{episodeLabel}</span>
          </div>
          <div className="ac-detail-row">
            <span className="ac-detail-label">Date</span>
            <span className="ac-detail-value">{session.sessionDate}</span>
          </div>
          <div className="ac-detail-row">
            <span className="ac-detail-label">Time</span>
            <span className="ac-detail-value">
              {session.startTime} – {session.endTime}
            </span>
          </div>
        </div>

        <div className={`ac-status ac-status--${info.cls}`}>
          <span className={`ac-status-dot ac-status-dot--${info.cls}`} />
          {info.label}
        </div>

        {actionError && <p className="ac-error">{actionError}</p>}

        {isAwaiting ? (
          <div className="ac-actions">
            <button
              className="ac-btn ac-btn--confirm"
              onClick={() => handleAction("confirm")}
              disabled={actionLoading}
            >
              {actionLoading ? "Submitting…" : "Confirm Session"}
            </button>
            <button
              className="ac-btn ac-btn--decline"
              onClick={() => handleAction("decline")}
              disabled={actionLoading}
            >
              {actionLoading ? "Submitting…" : "Decline Session"}
            </button>
          </div>
        ) : (
          <p className="ac-resolved-note">
            {session.status === "CONFIRMED" && "Thank you — your availability has been confirmed."}
            {session.status === "DECLINED" && "This request has been declined. No further action is needed."}
            {session.status === "CANCELLED" && "This session was cancelled by the studio. No action is needed."}
            {session.status === "COMPLETED" && "This session has already taken place."}
          </p>
        )}
      </div>
    </div>
  );
}

export default ArtistConfirmation;