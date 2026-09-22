import { useCallback, useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import axiosClient from "../api/axiosClient";
import "./Dashboard.css";

function Dashboard() {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(true);
  const [data, setData] = useState({
    active: [],
    upcoming: [],
    completed: [],
    delayed: [],
    upcomingSessions: [],
    pendingConfirmations: [],
    attention: [],
    briefing: null,
  });

  const loadDashboard = useCallback(async () => {
    setLoading(true);
    try {
      const [
        activeRes,
        upcomingRes,
        completedRes,
        delayedRes,
        sessionsRes,
        pendingRes,
        attentionRes,
        briefingRes,
      ] = await Promise.all([
        axiosClient.get("/projects/active"),
        axiosClient.get("/projects/upcoming"),
        axiosClient.get("/projects/completed"),
        axiosClient.get("/projects/delayed"),
        axiosClient.get("/sessions/upcoming"),
        axiosClient.get("/sessions/pending-confirmations"),
        axiosClient.get("/production/attention"),
        axiosClient.get("/production/briefing"),
      ]);

      setData({
        active: activeRes.data,
        upcoming: upcomingRes.data,
        completed: completedRes.data,
        delayed: delayedRes.data,
        upcomingSessions: sessionsRes.data,
        pendingConfirmations: pendingRes.data,
        attention: attentionRes.data,
        briefing: briefingRes.data,
      });
    } catch (err) {
      console.error("Failed to load dashboard data", err);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadDashboard();
  }, [loadDashboard]);

  const recordingNow = data.active.filter(
    (p) => p.stage === "RECORDING"
  ).length;

  const allProjects = [
    ...data.active,
    ...data.upcoming,
    ...data.delayed,
    ...data.completed,
  ];

  const briefingText = data.briefing
    ? `You have ${data.briefing.totalProjects} project${
        data.briefing.totalProjects === 1 ? "" : "s"
      } in the system — ${data.briefing.activeProjects} active, ${
        data.briefing.completedProjects
      } completed, and ${data.briefing.delayedProjects} delayed. ${
        data.briefing.pendingConfirmations
      } artist confirmation${
        data.briefing.pendingConfirmations === 1 ? " is" : "s are"
      } pending, with ${data.briefing.upcomingSessions} session${
        data.briefing.upcomingSessions === 1 ? "" : "s"
      } coming up.`
    : null;

  const stageStyle = (stage) => {
    const map = {
      CONFIRMED: { cls: "sage", label: "Confirmed" },
      COMPLETED: { cls: "sage", label: "Completed" },
      AWAITING_CONFIRMATION: { cls: "amber", label: "Pending" },
      UPCOMING: { cls: "rose", label: "Upcoming" },
      RECORDING: { cls: "terracotta", label: "Recording" },
      EDITING: { cls: "rose", label: "Editing" },
      PRE_PRODUCTION: { cls: "rose", label: "Pre-Production" },
      CASTING: { cls: "rose", label: "Casting" },
      REVIEW: { cls: "amber", label: "Review" },
      DELAYED: { cls: "terracotta", label: "Delayed" },
      DECLINED: { cls: "terracotta", label: "Declined" },
      CANCELLED: { cls: "muted", label: "Cancelled" },
    };

    return map[stage] || { cls: "muted", label: stage };
  };

  const priorityStyle = (priority) => {
    const map = {
      HIGH: { cls: "terracotta", label: "Important" },
      MEDIUM: { cls: "amber", label: "Warning" },
      LOW: { cls: "sage", label: "Note" },
    };

    return map[priority] || { cls: "muted", label: priority };
  };

  return (
    <div className="dp-page">

      {/* ================= HEADER ================= */}
      <header className="dp-header">
        <div className="dp-header__content">
          <div className="dp-eyebrow">
            <span className="dp-eyebrow__dot" />
            Studio overview
          </div>

          <h1 className="dp-header__title">
            Good morning
          </h1>

          <p className="dp-header__subtitle">
            Here's what's happening in your studio today.
          </p>
        </div>

        <div className="dp-header__actions">
          <button
            className="dp-btn dp-btn--soft"
            onClick={loadDashboard}
            disabled={loading}
          >
            <span className={loading ? "dp-spin" : ""}>↻</span>
            {loading ? "Refreshing…" : "Refresh"}
          </button>

          <button className="dp-btn dp-btn--outline">
            <span>＋</span>
            New Project
          </button>

          <button
            className="dp-btn dp-btn--primary"
            onClick={() => navigate("/voice")}
          >
            <span className="dp-btn__spark">✦</span>
            Talk to Aozene
            <span className="dp-btn__arrow">→</span>
          </button>
        </div>
      </header>

      {/* ================= KPI CARDS ================= */}
      <section className="dp-kpi-grid">

        <div className="dp-kpi-card dp-kpi-card--terracotta">
          <div className="dp-kpi-card__top">
            <div className="dp-kpi-card__icon">◈</div>
            <span className="dp-kpi-card__label">
              Active Projects
            </span>
          </div>

          <div className="dp-kpi-card__number">
            {data.active.length}
          </div>

          <p className="dp-kpi-card__desc">
            Currently in production
          </p>

          <div className="dp-kpi-card__bottom-line" />
        </div>

        <div className="dp-kpi-card dp-kpi-card--amber">
          <div className="dp-kpi-card__top">
            <div className="dp-kpi-card__icon">◉</div>
            <span className="dp-kpi-card__label">
              Recording Sessions
            </span>
          </div>

          <div className="dp-kpi-card__number">
            {recordingNow}
          </div>

          <p className="dp-kpi-card__desc">
            Projects in the recording stage
          </p>

          <div className="dp-kpi-card__bottom-line" />
        </div>

        <div className="dp-kpi-card dp-kpi-card--rose">
          <div className="dp-kpi-card__top">
            <div className="dp-kpi-card__icon">◷</div>
            <span className="dp-kpi-card__label">
              Pending Confirmations
            </span>
          </div>

          <div className="dp-kpi-card__number">
            {data.pendingConfirmations.length}
          </div>

          <p className="dp-kpi-card__desc">
            Awaiting artist response
          </p>

          <div className="dp-kpi-card__bottom-line" />
        </div>

        <div className="dp-kpi-card dp-kpi-card--sage">
          <div className="dp-kpi-card__top">
            <div className="dp-kpi-card__icon">✦</div>
            <span className="dp-kpi-card__label">
              Needs Attention
            </span>
          </div>

          <div className="dp-kpi-card__number">
            {data.attention.length}
          </div>

          <p className="dp-kpi-card__desc">
            Items to review
          </p>

          <div className="dp-kpi-card__bottom-line" />
        </div>

      </section>

      {/* ================= AI BRIEFING ================= */}
      <section className="dp-briefing">

        <div className="dp-briefing__glow" />

        <div className="dp-briefing__content">

          <div className="dp-briefing__label">
            <span className="dp-briefing__spark">✦</span>
            AI PRODUCTION BRIEFING
            <span className="dp-briefing__live">
              LIVE
            </span>
          </div>

          {briefingText ? (
            <p className="dp-briefing__text">
              {briefingText}
            </p>
          ) : (
            <p className="dp-briefing__text dp-briefing__text--empty">
              {loading
                ? "Gathering your studio's briefing…"
                : "No briefing data is available yet."}
            </p>
          )}

          <button
            className="dp-briefing__button"
            onClick={() => navigate("/voice")}
          >
            <span>✦</span>
            Ask DubPilot
            <span>→</span>
          </button>

        </div>

        <div className="dp-briefing__orb">
          <div className="dp-briefing__orb-inner">
            ✦
          </div>
        </div>

      </section>

      {/* ================= MAIN CONTENT ================= */}
      <div className="dp-main-grid">

        {/* ================= LEFT COLUMN ================= */}
        <div className="dp-main-grid__left">
                    {/* ---------- Pending Artist Confirmations ---------- */}
          <section className="dp-panel">

            <div className="dp-panel__header">
              <div>
                <p className="dp-section-kicker">
                  ARTIST RESPONSES
                </p>

                <h2 className="dp-panel__title">
                  Pending Artist Confirmations
                </h2>
              </div>

              <span className="dp-panel__count">
                {data.pendingConfirmations.length} pending
              </span>
            </div>

            {data.pendingConfirmations.length === 0 ? (
              <div className="dp-empty dp-empty--small">
                <div className="dp-empty__icon">
                  ◷
                </div>

                <p className="dp-empty__title">
                  No pending artist confirmations.
                </p>
              </div>
            ) : (
              <div className="dp-confirmation-list">
                {data.pendingConfirmations.map((s) => {
                  const episodeLabel = s.episode
                    ? `${
                        s.episode.episodeNumber
                          ? `Episode ${s.episode.episodeNumber}`
                          : "Episode"
                      }${s.episode.title ? ` — ${s.episode.title}` : ""}`
                    : "Unknown episode";

                  return (
                    <div key={s.id} className="dp-confirmation-card">

                      <div className="dp-confirmation-card__top">
                        <div>
                          <p className="dp-confirmation-card__artist">
                            {s.artist?.name || "Unknown artist"}
                          </p>
                          <p className="dp-confirmation-card__meta">
                            {s.episode?.project?.name || "Unknown project"}
                            {" · "}
                            {episodeLabel}
                          </p>
                        </div>

                        <span className="dp-badge dp-badge--amber">
                          Awaiting Confirmation
                        </span>
                      </div>

                      <div className="dp-confirmation-card__details">
                        <span>{s.sessionDate}</span>
                        <span className="dp-confirmation-card__dot" />
                        <span>{s.startTime} – {s.endTime}</span>
                      </div>

                      <button
                        className="dp-confirmation-card__btn"
                        onClick={() => navigate(`/artist-confirmation/${s.id}`)}
                      >
                        View Confirmation Page
                        <span className="dp-confirmation-card__arrow">→</span>
                      </button>

                    </div>
                  );
                })}
              </div>
            )}

          </section>

          {/* ---------- Production Overview ---------- */}
          <section className="dp-panel">

            <div className="dp-panel__header">
              <div>
                <p className="dp-section-kicker">
                  YOUR WORKSPACE
                </p>

                <h2 className="dp-panel__title">
                  Production Overview
                </h2>
              </div>

              <span className="dp-panel__count">
                {allProjects.length} projects
              </span>
            </div>

            {allProjects.length === 0 && !loading ? (
              <div className="dp-empty">
                <div className="dp-empty__icon">
                  ◈
                </div>

                <p className="dp-empty__title">
                  No projects yet
                </p>

                <p className="dp-empty__subtitle">
                  Your production workspace will appear here once
                  you create your first project.
                </p>
              </div>
            ) : (
              <div className="dp-project-list">
                {allProjects.map((p) => {
                  const pct = p.progressPercentage ?? 0;
                  const s = stageStyle(p.stage);

                  return (
                    <div
                      key={p.id}
                      className="dp-project-row"
                    >
                      <div className="dp-project-row__top">

                        <div className="dp-project-row__identity">
                          <div className={`dp-project-row__indicator dp-project-row__indicator--${s.cls}`} />

                          <div>
                            <p className="dp-project-row__name">
                              {p.name}
                            </p>

                            <p className="dp-project-row__meta">
                              {p.clientName || "No client set"}
                              {p.deadline
                                ? ` · Due ${p.deadline}`
                                : ""}
                            </p>
                          </div>
                        </div>

                        <div className="dp-project-row__right">

                          <span
                            className={`dp-badge dp-badge--${s.cls}`}
                          >
                            {s.label}
                          </span>

                          <span className="dp-project-row__pct">
                            {pct}%
                          </span>

                        </div>
                      </div>

                      <div className="dp-progress-track">
                        <div
                          className={`dp-progress-fill dp-progress-fill--${s.cls}`}
                          style={{ width: `${pct}%` }}
                        />
                      </div>

                    </div>
                  );
                })}
              </div>
            )}

          </section>

          {/* ---------- Upcoming Sessions ---------- */}
          <section className="dp-panel">

            <div className="dp-panel__header">
              <div>
                <p className="dp-section-kicker">
                  STUDIO SCHEDULE
                </p>

                <h2 className="dp-panel__title">
                  Upcoming Sessions
                </h2>
              </div>

              <span className="dp-panel__count">
                {data.upcomingSessions.length} scheduled
              </span>
            </div>

            {data.upcomingSessions.length === 0 ? (
              <div className="dp-empty dp-empty--small">
                <div className="dp-empty__icon">
                  ◷
                </div>

                <p className="dp-empty__title">
                  No upcoming sessions
                </p>

                <p className="dp-empty__subtitle">
                  No recording sessions are currently scheduled.
                </p>
              </div>
            ) : (
              <div className="dp-session-list">
                {data.upcomingSessions.map((s) => {
                  const st = stageStyle(s.status);

                  return (
                    <div
                      key={s.id}
                      className="dp-session-row"
                    >

                      <div className="dp-session-row__time">
                        {s.startTime}
                      </div>

                      <div className="dp-session-row__line">
                        <div className="dp-session-row__dot" />
                      </div>

                      <div className="dp-session-row__body">
                        <p className="dp-session-row__title">
                          {s.episode?.project?.name ||
                            "Unknown project"}
                          {" — "}
                          {s.episode?.title || "Episode"}
                        </p>

                        <p className="dp-session-row__meta">
                          {s.artist?.name || "Unassigned"}
                          {" · "}
                          {s.sessionDate}
                        </p>
                      </div>

                      <span
                        className={`dp-badge dp-badge--${st.cls}`}
                      >
                        {st.label}
                      </span>

                    </div>
                  );
                })}
              </div>
            )}

          </section>

        </div>

        {/* ================= NEEDS ATTENTION ================= */}
        <section className="dp-panel dp-panel--attention">

          <div className="dp-panel__header">
            <div>
              <p className="dp-section-kicker">
                PRODUCTION PULSE
              </p>

              <h2 className="dp-panel__title">
                Needs Attention
              </h2>
            </div>

            <div className="dp-attention-icon">
              ✦
            </div>
          </div>

          {data.attention.length === 0 ? (
            <div className="dp-empty dp-empty--attention">
              <div className="dp-success-icon">
                ✓
              </div>

              <p className="dp-empty__title">
                Everything looks good
              </p>

              <p className="dp-empty__subtitle">
                There are no pending confirmations or production
                issues requiring your attention.
              </p>
            </div>
          ) : (
            <div className="dp-attention-list">
              {data.attention.map((item, idx) => {
                const p = priorityStyle(item.priority);

                return (
                  <div
                    key={idx}
                    className={`dp-attention-item dp-attention-item--${p.cls}`}
                  >

                    <div className="dp-attention-item__indicator" />

                    <div className="dp-attention-item__content">

                      <div className="dp-attention-item__top">
                        <span
                          className={`dp-badge dp-badge--${p.cls} dp-badge--sm`}
                        >
                          {p.label}
                        </span>
                      </div>

                      <p className="dp-attention-item__title">
                        {item.title}
                      </p>

                      <p className="dp-attention-item__desc">
                        {item.description}
                      </p>

                    </div>

                    <span className="dp-attention-item__arrow">
                      →
                    </span>

                  </div>
                );
              })}
            </div>
          )}

        </section>

      </div>

    </div>
  );
}

export default Dashboard;