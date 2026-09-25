import { useEffect, useRef, useState } from "react";
import axiosClient from "../api/axiosClient";
import "./VoiceAgent.css";

const SYSTEM_PROMPT = `You are Aozene, a voice-native production coordinator for a dubbing studio.

The backend database is the source of truth. Production data is live and can change during this conversation. If the database is currently empty of something the manager asks about, say so plainly instead of making anything up - for example "Your production database is currently empty. There are no active projects, sessions, or pending confirmations to report."

You can:
- list projects (all, active, upcoming, completed, or delayed)
- create projects
- update project status and progress
- check the status and summary of one project
- provide production briefings and a list of items needing attention
- list voice artists and active voice artists
- add new voice artists
- update an existing artist's phone, specialization, or active status
- check and record an artist's availability
- find available artists for a specific recording time
- create a recording request asking an artist about a specific date and time
- check the status of a recording request
- find replacement artists for a declined or unavailable request
- list upcoming recording sessions and pending artist confirmations
- list recent production notes
- delete a project, episode, or production note
- deactivate or activate a voice artist
- cancel a recording session
- list episodes for a project and create new episodes

Rules you must always follow:
1. Never invent database information - project ids, artist ids, episode ids, session ids, note ids, names, stages, progress values, deadlines, dates, times, availability, session status, or notes.
2. When a user asks about current production or artists, use the appropriate backend tool instead of relying on information from earlier in this conversation, since data can change at any time.
3. Before calling update_project, get_project_status, update_artist, get_artist_availability, update_artist_availability, request_artist_availability, find_available_artists, delete_project, deactivate_artist, delete_episode, delete_production_note, get_episodes, or create_episode, you must first call get_projects and/or get_artists to find the correct id for the project, artist, or episode the user named. Never guess or reuse an id from memory.
4. If the user names a project, artist, or episode that isn't in the data you retrieved, tell them it wasn't found - don't guess which one they meant.
5. If more than one project or artist could match what the user said, ask a short clarifying question before acting.
6. When creating a project or artist, only pass values the user actually said. If they didn't mention a field, leave it out - do not invent one.
7. If the user wants to add an artist but hasn't given an email address, ask for it. Never invent an email address.
8. When the user asks to change production or artist data, call the appropriate tool. Only report success after the backend confirms it - if the backend returns an error, explain it naturally and don't claim the change happened.
9. The database is the source of truth for artists and availability. Recorded availability is information stored in the system - it does not mean a recording session has been personally confirmed by the artist.
10. When checking an artist's availability, look at the most recently recorded entry for the requested date and time. If no recorded entry covers the requested time, say plainly that no recorded availability was found - do not guess.
11. When the manager wants to schedule a recording, determine the project, episode, date and time first. Never assume an artist is available.
12. If the manager names a specific artist, check the backend (using request_artist_availability, which performs the availability and conflict check before creating anything) before creating the request. If the artist has a conflict, explain the conflict using the reason the backend gives you, and offer to find another artist with find_available_artists. Do not create a conflicting session.
13. If the manager asks for any suitable artist rather than a specific one, use find_available_artists with the project, episode, date, and time - and specialization if they mentioned one.
14. Never claim an artist is available unless the backend confirms it. Never claim a recording is confirmed unless the session status is CONFIRMED.
15. If a recording request is created successfully, tell the manager the artist still needs to confirm. Never say the artist is confirmed.
16. If an artist has declined a request, report the decline honestly and offer to find replacement artists with find_replacement_artists, using the declined session's id.
17. Before calling request_artist_availability, make sure you know the artist, the project, the episode, and the exact date and time. If any of these is missing, ask the manager for it - never invent a date, time, artist, project, or episode.
18. A newly scheduled or requested session is AWAITING_CONFIRMATION unless explicitly confirmed through the confirmation workflow.
19. Never claim an artist has confirmed a recording session unless the session status is CONFIRMED - use get_session_status to check current status rather than relying on what you remember from earlier in the conversation.
20. Clearly distinguish between these session states when speaking: UPCOMING (a project stage, not a session state), AWAITING_CONFIRMATION (requested, not yet answered), CONFIRMED (artist has agreed), DECLINED (artist said no), CANCELLED (called off), and for projects: DELAYED and COMPLETED. Never blur these together or imply a stronger status than the backend reports.
21. For "which project is closest to completion" or similar ranking questions, call get_projects and compare the progressPercentage values yourself - do not guess or remember old numbers.
22. For "what happened recently" or "any recent notes", use get_recent_notes.
23. Be concise and natural, because your responses are spoken aloud. Keep replies short but include the real numbers and names from the backend data.
24. If information is missing, ask the manager a short clarification question.
25. Do not claim to have sent SMS, WhatsApp messages, emails, or phone calls, because those integrations do not exist. If you create a recording request, you may say that a confirmation page has been created for the artist - not that a message was sent to them.
26. Destructive actions - deleting a project, episode, or production note; deactivating an artist; or cancelling a recording session - must NEVER be performed without the user's explicit, clear confirmation in this same conversation. When the user asks for one of these actions: first identify the exact record using the appropriate lookup tool (get_projects, get_artists, get_episodes, or get_session_status). If the name is ambiguous or multiple records could match, ask the user to clarify - never guess. Then tell the user plainly what you are about to do and that it cannot be easily undone, and ask "Do you want me to do this?" or similar. Only call the destructive tool after the user clearly says yes (e.g. "yes", "confirm", "do it", "go ahead"). If the user says no, cancel, or anything else that isn't a clear yes, do not call the tool - acknowledge and stop. Each destructive tool requires a confirmed: true parameter - only set this to true after the user has explicitly confirmed in this conversation; never set it to true on your own initiative.
27. Never claim a deletion, deactivation, or cancellation succeeded unless the backend actually returned success. If the backend returns an error (for example, because a project has episodes, or an artist has session history), explain the real reason to the user in plain language rather than saying the action worked.
28. When creating a project, collect the project name, client name, and deadline before calling create_project - these are the minimum details a real project needs. Description, stage, and progress are optional; if not mentioned, leave stage and progress unset (the backend defaults them to UPCOMING and 0) and leave description out.
29. Immediately after a project is successfully created, ask the manager naturally whether it has episodes to add - for example "Project [name] has been created. Does it have episodes you'd like me to add?" Do not invent episodes on your own.
30. To add an episode, you need at minimum an episode number and a title for it - if the manager only says "add 10 episodes" without giving numbers or titles, ask them to provide each one (they can give you several at once, e.g. "episode 1 The Beginning, episode 2 The Journey"). Description is optional.
31. When the manager wants to add multiple episodes in one request, call create_episode once per episode, using the same project id each time. Confirm each one only after the backend actually returns success for that specific episode.
32. If create_episode fails because the episode number already exists for that project, tell the manager plainly (for example, "Episode 1 already exists for this project") rather than claiming it was added or silently skipping it.
33. To answer "how many episodes does [project] have", "what are the episodes in [project]", or "show me episode [number]", call get_episodes with the resolved project id and read the real returned list - for a specific episode number, look for it in that list yourself. If it isn't there, say so rather than guessing.
34. Episode count has nothing to do with whether a project is complete. A project's completion status depends only on its stage and progress percentage as reported by get_project_status or get_projects - never assume a project is more or less complete because of how many episodes it has.
35. Always use numeric digits for numbers instead of spelling them out.`;

const GREETING = "Hi, I'm Aozene. Ask me for today's production briefing whenever you're ready.";

const TOOLS = [
  {
    type: "function",
    name: "get_production_briefing",
    description:
      "Get today's real production briefing from the studio's database: total projects, active projects, completed projects, delayed projects, pending artist confirmations, upcoming recording sessions, and items needing attention. Use this whenever the manager asks for a production briefing, status update, overview of the studio, or what needs attention overall.",
    parameters: { type: "object", properties: {}, required: [] },
  },
  {
    type: "function",
    name: "get_production_attention",
    description:
      "Get the current list of production items needing the manager's attention, each with a type, priority (HIGH, MEDIUM, or LOW), title, and description - covering delayed projects, pending artist confirmations, declined sessions, approaching deadlines, upcoming sessions, and recent notes. Use this when the manager asks 'what needs my attention'.",
    parameters: { type: "object", properties: {}, required: [] },
  },
  {
    type: "function",
    name: "get_projects",
    description:
      "Get the current list of production projects from the backend, including each project's id, name, stage, progress percentage, and deadline. Always call this first to resolve a project name to its id before calling update_project, get_project_status, find_available_artists, request_artist_availability, delete_project, get_episodes, or create_episode. Also use this to answer questions like 'which projects are in production' or 'which project is closest to completion' by looking at the returned stage and progressPercentage values yourself.",
    parameters: { type: "object", properties: {}, required: [] },
  },
  {
    type: "function",
    name: "create_project",
    description:
      "Create a new production project in the database. Only pass values the user actually said - never invent a description, client name, or deadline the user didn't mention.",
    parameters: {
      type: "object",
      properties: {
        name: { type: "string", description: "The project's name, e.g. 'Starlight Chronicles'." },
        description: { type: "string", description: "Short description of the project, only if the user gave one." },
        clientName: { type: "string", description: "The client or studio name, only if the user gave one." },
        deadline: { type: "string", format: "date", description: "Deadline in YYYY-MM-DD format, only if the user gave one." },
      },
      required: ["name"],
    },
  },
  {
    type: "function",
    name: "update_project",
    description:
      "Update an existing project's stage, progress, deadline, description, or client name. You must already have the project's id from get_projects - never guess it. Only include fields the user actually asked to change.",
    parameters: {
      type: "object",
      properties: {
        projectId: { type: "integer", description: "The numeric id of the project, from get_projects." },
        stage: {
          type: "string",
          enum: ["UPCOMING", "PRE_PRODUCTION", "CASTING", "RECORDING", "EDITING", "REVIEW", "COMPLETED", "DELAYED"],
          description: "The new production stage, only if the user asked to change it.",
        },
        progressPercentage: { type: "number", description: "The new progress percentage, 0 to 100, only if the user asked to change it." },
        deadline: { type: "string", format: "date", description: "The new deadline in YYYY-MM-DD format, only if the user asked to change it." },
        description: { type: "string", description: "The new project description, only if the user asked to change it." },
        clientName: { type: "string", description: "The new client name, only if the user asked to change it." },
      },
      required: ["projectId"],
    },
  },
  {
    type: "function",
    name: "get_project_status",
    description:
      "Get the current status, stage, progress, and deadline of one specific project by id. Use this to answer 'how much progress has [project] made'. You must already have the project's id from get_projects - never guess it.",
    parameters: {
      type: "object",
      properties: { projectId: { type: "integer", description: "The numeric id of the project, from get_projects." } },
      required: ["projectId"],
    },
  },
  {
    type: "function",
    name: "get_artists",
    description:
      "Get all current voice artists from the backend, including each artist's id, name, email, phone, specialization, and active status. Always call this first to resolve an artist's name to their id before calling update_artist, get_artist_availability, update_artist_availability, request_artist_availability, or deactivate_artist - never guess or reuse an id from earlier in the conversation.",
    parameters: { type: "object", properties: {}, required: [] },
  },
  {
    type: "function",
    name: "get_active_artists",
    description: "Get only the voice artists currently marked active in the backend.",
    parameters: { type: "object", properties: {}, required: [] },
  },
  {
    type: "function",
    name: "create_artist",
    description:
      "Add a new voice artist to the database. An email address is required - if the user hasn't given one, ask for it instead of calling this tool. Never invent an email address.",
    parameters: {
      type: "object",
      properties: {
        name: { type: "string", description: "The artist's full name." },
        email: { type: "string", description: "The artist's email address. Required - ask if not given." },
        phone: { type: "string", description: "The artist's phone number, only if the user gave one." },
        specialization: { type: "string", description: "The artist's specialization, e.g. 'Female Lead', 'Narration', only if the user gave one." },
      },
      required: ["name", "email"],
    },
  },
  {
    type: "function",
    name: "update_artist",
    description:
      "Update an existing artist's phone number, specialization, or active status. You must already have the artist's id from get_artists - never guess it. Only include fields the user actually asked to change.",
    parameters: {
      type: "object",
      properties: {
        artistId: { type: "integer", description: "The numeric id of the artist, from get_artists." },
        phone: { type: "string", description: "The new phone number, only if the user asked to change it." },
        specialization: { type: "string", description: "The new specialization, only if the user asked to change it." },
        active: { type: "boolean", description: "The new active status, only if the user asked to change it." },
      },
      required: ["artistId"],
    },
  },
  {
    type: "function",
    name: "get_artist_availability",
    description:
      "Get the recorded availability entries for one artist, newest first. You must already have the artist's id from get_artists - never guess it. If asked whether an artist is available at a specific time, check the newest entry that covers that date and time. If none covers it, say plainly that no recorded availability was found - do not guess.",
    parameters: {
      type: "object",
      properties: { artistId: { type: "integer", description: "The numeric id of the artist, from get_artists." } },
      required: ["artistId"],
    },
  },
  {
    type: "function",
    name: "update_artist_availability",
    description:
      "Record a new availability entry for an artist - either marking them available or unavailable for a specific date and time range. You must already have the artist's id from get_artists. Never invent dates or times - if the user hasn't given enough date/time information, ask for clarification instead of calling this tool.",
    parameters: {
      type: "object",
      properties: {
        artistId: { type: "integer", description: "The numeric id of the artist, from get_artists." },
        startDateTime: { type: "string", format: "date-time", description: "Start of the availability window, in ISO format, e.g. 2026-09-16T14:00:00." },
        endDateTime: { type: "string", format: "date-time", description: "End of the availability window, in ISO format, e.g. 2026-09-16T17:00:00." },
        availabilityStatus: { type: "string", enum: ["AVAILABLE", "UNAVAILABLE"], description: "Whether this entry marks the artist as available or unavailable for the window." },
      },
      required: ["artistId", "startDateTime", "endDateTime", "availabilityStatus"],
    },
  },
  {
    type: "function",
    name: "find_available_artists",
    description:
      "Find active voice artists who are available for a requested recording time, based on their recorded availability and existing session conflicts. Use this when the manager asks for any suitable artist rather than naming one specifically. You must already have the project's id from get_projects and the episode's id before calling this.",
    parameters: {
      type: "object",
      properties: {
        projectId: { type: "integer", description: "The numeric id of the project, from get_projects." },
        episodeId: { type: "integer", description: "The numeric id of the episode." },
        startDateTime: { type: "string", format: "date-time", description: "Requested start time, in ISO format, e.g. 2026-09-16T15:00:00." },
        endDateTime: { type: "string", format: "date-time", description: "Requested end time, in ISO format, e.g. 2026-09-16T17:00:00." },
        specialization: { type: "string", description: "Optional specialization filter, e.g. 'Female Lead', only if the manager mentioned one." },
      },
      required: ["projectId", "episodeId", "startDateTime", "endDateTime"],
    },
  },
  {
    type: "function",
    name: "request_artist_availability",
    description:
      "Create a recording request for a specific artist, project, episode, date and time. This does not assume the artist is available - the request is created with status AWAITING_CONFIRMATION and remains that way until the artist responds on their confirmation page, or the backend returns a conflict if the artist is unavailable. You must already have the artist's id (from get_artists), the project's id (from get_projects), and the episode's id before calling this. Never invent a date or time - ask the manager if it's unclear.",
    parameters: {
      type: "object",
      properties: {
        artistId: { type: "integer", description: "The numeric id of the artist, from get_artists." },
        projectId: { type: "integer", description: "The numeric id of the project, from get_projects." },
        episodeId: { type: "integer", description: "The numeric id of the episode the session is for." },
        startDateTime: { type: "string", format: "date-time", description: "Requested start of the recording session, in ISO format, e.g. 2026-09-16T15:00:00." },
        endDateTime: { type: "string", format: "date-time", description: "Requested end of the recording session, in ISO format, e.g. 2026-09-16T17:00:00." },
      },
      required: ["artistId", "projectId", "episodeId", "startDateTime", "endDateTime"],
    },
  },
  {
    type: "function",
    name: "get_session_status",
    description:
      "Get the current status and details of a specific recording session by id, including the artist, episode, project, date, time, and status. Use this to check whether a session is still AWAITING_CONFIRMATION, has been CONFIRMED, DECLINED, or CANCELLED - never assume based on earlier conversation.",
    parameters: {
      type: "object",
      properties: { sessionId: { type: "integer", description: "The numeric id of the recording session." } },
      required: ["sessionId"],
    },
  },
  {
    type: "function",
    name: "find_replacement_artists",
    description:
      "Find suitable replacement artists for a declined or unavailable recording session, automatically excluding the currently assigned artist. You must already have the session's id.",
    parameters: {
      type: "object",
      properties: { sessionId: { type: "integer", description: "The numeric id of the recording session to find a replacement for." } },
      required: ["sessionId"],
    },
  },
  {
    type: "function",
    name: "get_upcoming_sessions",
    description:
      "Get all upcoming recording sessions (excluding declined, cancelled, and completed ones), sorted by date and time. Use this when the manager asks 'what sessions are coming up' or similar.",
    parameters: { type: "object", properties: {}, required: [] },
  },
  {
    type: "function",
    name: "get_pending_confirmations",
    description:
      "Get all recording sessions currently awaiting artist confirmation, sorted by date and time. Use this when the manager asks 'which artist confirmations are pending'.",
    parameters: { type: "object", properties: {}, required: [] },
  },
  {
    type: "function",
    name: "get_recent_notes",
    description: "Get the most recent production notes across all projects, newest first. Use this when the manager asks what happened recently in production.",
    parameters: { type: "object", properties: {}, required: [] },
  },
  {
    type: "function",
    name: "delete_project",
    description:
      "Permanently delete a project. This is destructive and only works if the project has no episodes or production notes - the backend will explain why if it can't be deleted. NEVER call this with confirmed: true unless the user has just explicitly said yes to deleting this specific project in this conversation.",
    parameters: {
      type: "object",
      properties: {
        projectId: { type: "integer", description: "The numeric id of the project, from get_projects." },
        confirmed: { type: "boolean", description: "Must be true, and must only be set to true after the user has explicitly confirmed deletion out loud." },
      },
      required: ["projectId", "confirmed"],
    },
  },
  {
    type: "function",
    name: "deactivate_artist",
    description:
      "Deactivate a voice artist so they can no longer be selected for new scheduling or recording requests. Their existing session history is preserved. NEVER call this with confirmed: true unless the user has just explicitly said yes to deactivating this specific artist in this conversation.",
    parameters: {
      type: "object",
      properties: {
        artistId: { type: "integer", description: "The numeric id of the artist, from get_artists." },
        confirmed: { type: "boolean", description: "Must be true, and must only be set to true after the user has explicitly confirmed deactivation out loud." },
      },
      required: ["artistId", "confirmed"],
    },
  },
  {
    type: "function",
    name: "delete_episode",
    description:
      "Permanently delete an episode. This is destructive and only works if the episode has no recording sessions or production notes - the backend will explain why if it can't be deleted. NEVER call this with confirmed: true unless the user has just explicitly said yes to deleting this specific episode in this conversation.",
    parameters: {
      type: "object",
      properties: {
        episodeId: { type: "integer", description: "The numeric id of the episode." },
        confirmed: { type: "boolean", description: "Must be true, and must only be set to true after the user has explicitly confirmed deletion out loud." },
      },
      required: ["episodeId", "confirmed"],
    },
  },
  {
    type: "function",
    name: "cancel_session",
    description:
      "Cancel a recording session. The session record is preserved with status CANCELLED for production history - it is not deleted. Only works on sessions currently AWAITING_CONFIRMATION or CONFIRMED. NEVER call this with confirmed: true unless the user has just explicitly said yes to cancelling this specific session in this conversation, or there is no ambiguity about which session they mean.",
    parameters: {
      type: "object",
      properties: {
        sessionId: { type: "integer", description: "The numeric id of the recording session." },
        confirmed: { type: "boolean", description: "Must be true, and must only be set to true after the user has explicitly confirmed cancellation out loud." },
      },
      required: ["sessionId", "confirmed"],
    },
  },
  {
    type: "function",
    name: "delete_production_note",
    description:
      "Permanently delete a production note. NEVER call this with confirmed: true unless the user has just explicitly said yes to deleting this specific note in this conversation.",
    parameters: {
      type: "object",
      properties: {
        noteId: { type: "integer", description: "The numeric id of the production note." },
        confirmed: { type: "boolean", description: "Must be true, and must only be set to true after the user has explicitly confirmed deletion out loud." },
      },
      required: ["noteId", "confirmed"],
    },
  },
  {
    type: "function",
    name: "get_episodes",
    description:
      "Get all episodes for a specific project, including each episode's id, episode number, title, and description. Use this to answer how many episodes a project has, list its episodes, or find a specific episode by number (look through the returned list yourself - there is no separate 'get one episode' tool). You must already have the project's id from get_projects.",
    parameters: {
      type: "object",
      properties: {
        projectId: { type: "integer", description: "The numeric id of the project, from get_projects." },
      },
      required: ["projectId"],
    },
  },
  {
    type: "function",
    name: "create_episode",
    description:
      "Create a new episode for an existing project. You must already have the project's id from get_projects. Never invent an episode number or title - ask the manager for whichever is missing. If the manager wants to add several episodes, call this tool once per episode. If the backend reports the episode number already exists for this project, tell the manager honestly rather than claiming success.",
    parameters: {
      type: "object",
      properties: {
        projectId: { type: "integer", description: "The numeric id of the project, from get_projects." },
        episodeNumber: { type: "integer", description: "The episode number, e.g. 1, 2, 3. Required - never guess." },
        title: { type: "string", description: "The episode's title. Required - never invent one." },
        description: { type: "string", description: "Optional short description, only if the manager gave one." },
      },
      required: ["projectId", "episodeNumber", "title"],
    },
  },
];

const SUGGESTIONS = [
  "Give me today's production briefing",
  "Which projects are delayed?",
  "What needs my attention?",
];

const STATE_TEXT = {
  idle: "Ready to listen",
  connecting: "Connecting…",
  ready: "Listening…",
  speaking: "Aozene is responding…",
  error: "Connection error",
  ended: "Convo ended",
};


const CONVERSATION_STORAGE_KEY = "Aozene_voice_conversation";

function loadStoredTranscript() {
  try {
    const raw = sessionStorage.getItem(CONVERSATION_STORAGE_KEY);
    if (!raw) return [];
    const parsed = JSON.parse(raw);
    if (!Array.isArray(parsed)) return [];
    // Basic shape check so corrupted/foreign data can't break rendering.
    return parsed.filter(
      (entry) => entry && typeof entry.speaker === "string" && typeof entry.text === "string"
    );
  } catch (err) {
    // Corrupted JSON or any other read error - fail safe, start empty.
    return [];
  }
}

function VoiceAgent() {
  const [status, setStatus] = useState("idle");
  const [transcript, setTranscript] = useState(loadStoredTranscript);
  const [errorMessage, setErrorMessage] = useState("");

  const wsRef = useRef(null);
  const audioCtxRef = useRef(null);
  const streamRef = useRef(null);
  const readyRef = useRef(false);
  const playbackTimeRef = useRef(0);
  const pendingToolsRef = useRef([]);


  useEffect(() => {
    try {
      sessionStorage.setItem(CONVERSATION_STORAGE_KEY, JSON.stringify(transcript));
    } catch (err) {
      // sessionStorage can throw in rare cases (quota exceeded, private
      // browsing restrictions). Persistence is a nice-to-have here, not
      // critical to the voice session itself, so we swallow this quietly
      // rather than breaking the UI.
    }
  }, [transcript]);

  const appendTranscript = (speaker, text) => {
    setTranscript((prev) => [...prev, { speaker, text }]);
  };

  const clearConversation = () => {
    setTranscript([]);
    try {
      sessionStorage.removeItem(CONVERSATION_STORAGE_KEY);
    } catch (err) {
      // ignore
    }
  };

  const playAgentAudio = async (base64Pcm) => {
  try {
    const audioCtx = audioCtxRef.current;

    if (!audioCtx) {
      console.error("AudioContext is not available");
      return;
    }

    // Make sure the browser has actually started the audio context.
    if (audioCtx.state === "suspended") {
      await audioCtx.resume();
    }

    const raw = atob(base64Pcm);
    const pcm16 = new Int16Array(raw.length / 2);

    for (let i = 0; i < pcm16.length; i++) {
      pcm16[i] =
        raw.charCodeAt(i * 2) |
        (raw.charCodeAt(i * 2 + 1) << 8);
    }

    const float32 = new Float32Array(pcm16.length);

    for (let i = 0; i < pcm16.length; i++) {
      float32[i] = pcm16[i] / 32768;
    }

    const buffer = audioCtx.createBuffer(
      1,
      float32.length,
      24000
    );

    buffer.getChannelData(0).set(float32);

    const source = audioCtx.createBufferSource();
    source.buffer = buffer;
    source.connect(audioCtx.destination);

    const now = audioCtx.currentTime;

    if (playbackTimeRef.current < now) {
      playbackTimeRef.current = now;
    }

    source.start(playbackTimeRef.current);

    playbackTimeRef.current += buffer.duration;

    console.log("AOZENE audio playing:", buffer.duration, "seconds");
  } catch (error) {
    console.error("AOZENE audio playback error:", error);
  }
};

  const executeTool = async (name, args) => {
    try {
      switch (name) {
        case "get_production_briefing": {
          const response = await axiosClient.get("/production/briefing");
          return response.data;
        }
        case "get_production_attention": {
          const response = await axiosClient.get("/production/attention");
          return response.data;
        }
        case "get_projects": {
          const response = await axiosClient.get("/projects");
          return response.data;
        }
        case "create_project": {
          const body = {};
          if (args.name) body.name = args.name;
          if (args.description) body.description = args.description;
          if (args.clientName) body.clientName = args.clientName;
          if (args.deadline) body.deadline = args.deadline;
          const response = await axiosClient.post("/projects", body);
          return response.data;
        }
        case "update_project": {
          const body = {};
          if (args.stage) body.stage = args.stage;
          if (args.progressPercentage !== undefined) body.progressPercentage = args.progressPercentage;
          if (args.deadline) body.deadline = args.deadline;
          if (args.description) body.description = args.description;
          if (args.clientName) body.clientName = args.clientName;
          const response = await axiosClient.put(`/projects/${args.projectId}`, body);
          return response.data;
        }
        case "get_project_status": {
          const response = await axiosClient.get(`/projects/${args.projectId}/status`);
          return response.data;
        }
        case "get_artists": {
          const response = await axiosClient.get("/artists");
          return response.data;
        }
        case "get_active_artists": {
          const response = await axiosClient.get("/artists/active");
          return response.data;
        }
        case "create_artist": {
          if (!args.email) {
            return { error: "An email address is required to add a new artist. Please ask the user for one." };
          }
          const body = { name: args.name, email: args.email };
          if (args.phone) body.phone = args.phone;
          if (args.specialization) body.specialization = args.specialization;
          const response = await axiosClient.post("/artists", body);
          return response.data;
        }
        case "update_artist": {
          const body = {};
          if (args.phone) body.phone = args.phone;
          if (args.specialization) body.specialization = args.specialization;
          if (args.active !== undefined) body.active = args.active;
          const response = await axiosClient.patch(`/artists/${args.artistId}`, body);
          return response.data;
        }
        case "get_artist_availability": {
          const response = await axiosClient.get(`/artists/${args.artistId}/availability`);
          return response.data;
        }
        case "update_artist_availability": {
          const body = { startDateTime: args.startDateTime, endDateTime: args.endDateTime, availabilityStatus: args.availabilityStatus };
          const response = await axiosClient.post(`/artists/${args.artistId}/availability`, body);
          return response.data;
        }
        case "find_available_artists": {
          const params = { projectId: args.projectId, episodeId: args.episodeId, startDateTime: args.startDateTime, endDateTime: args.endDateTime };
          if (args.specialization) params.specialization = args.specialization;
          const response = await axiosClient.get("/scheduling/available-artists", { params });
          return response.data;
        }
        case "request_artist_availability": {
          const [sessionDate, startTimeFull] = args.startDateTime.split("T");
          const [, endTimeFull] = args.endDateTime.split("T");
          const startTime = startTimeFull.slice(0, 5);
          const endTime = endTimeFull.slice(0, 5);

          const body = {
            artistId: args.artistId,
            projectId: args.projectId,
            episodeId: args.episodeId,
            sessionDate,
            startTime,
            endTime,
            notes: "Requested via Aozene voice agent - awaiting artist confirmation.",
          };

          const response = await axiosClient.post("/sessions", body);
          const created = response.data;
          const confirmationUrl = `${window.location.origin}/artist-confirmation/${created.id}`;

          return {
            sessionId: created.id,
            artist: created.artist ? { id: created.artist.id, name: created.artist.name } : null,
            project: created.episode?.project ? { id: created.episode.project.id, name: created.episode.project.name } : null,
            episode: created.episode ? { id: created.episode.id, title: created.episode.title } : null,
            startDateTime: args.startDateTime,
            endDateTime: args.endDateTime,
            status: created.status,
            confirmationUrl,
          };
        }
        case "get_session_status": {
          const response = await axiosClient.get(`/sessions/${args.sessionId}`);
          const session = response.data;
          return {
            sessionId: session.id,
            status: session.status,
            artist: session.artist ? { id: session.artist.id, name: session.artist.name } : null,
            project: session.episode?.project ? { id: session.episode.project.id, name: session.episode.project.name } : null,
            episode: session.episode ? { id: session.episode.id, title: session.episode.title } : null,
            sessionDate: session.sessionDate,
            startTime: session.startTime,
            endTime: session.endTime,
          };
        }
        case "find_replacement_artists": {
          const response = await axiosClient.get("/scheduling/replacement-artists", { params: { sessionId: args.sessionId } });
          return response.data;
        }
        case "get_upcoming_sessions": {
          const response = await axiosClient.get("/sessions/upcoming");
          return response.data;
        }
        case "get_pending_confirmations": {
          const response = await axiosClient.get("/sessions/pending-confirmations");
          return response.data;
        }
        case "get_recent_notes": {
          const response = await axiosClient.get("/notes/recent");
          return response.data;
        }
        case "delete_project": {
          if (args.confirmed !== true) {
            return { error: "Confirmation required. Ask the user to explicitly confirm deletion, then call this tool again with confirmed: true." };
          }
          await axiosClient.delete(`/projects/${args.projectId}`);
          return { success: true, deletedProjectId: args.projectId };
        }
        case "deactivate_artist": {
          if (args.confirmed !== true) {
            return { error: "Confirmation required. Ask the user to explicitly confirm deactivation, then call this tool again with confirmed: true." };
          }
          const response = await axiosClient.put(`/artists/${args.artistId}/deactivate`);
          return response.data;
        }
        case "delete_episode": {
          if (args.confirmed !== true) {
            return { error: "Confirmation required. Ask the user to explicitly confirm deletion, then call this tool again with confirmed: true." };
          }
          await axiosClient.delete(`/episodes/${args.episodeId}`);
          return { success: true, deletedEpisodeId: args.episodeId };
        }
        case "cancel_session": {
          if (args.confirmed !== true) {
            return { error: "Confirmation required. Ask the user to explicitly confirm cancellation, then call this tool again with confirmed: true." };
          }
          const response = await axiosClient.put(`/sessions/${args.sessionId}/cancel`);
          return response.data;
        }
        case "delete_production_note": {
          if (args.confirmed !== true) {
            return { error: "Confirmation required. Ask the user to explicitly confirm deletion, then call this tool again with confirmed: true." };
          }
          await axiosClient.delete(`/notes/${args.noteId}`);
          return { success: true, deletedNoteId: args.noteId };
        }
        case "get_episodes": {
          const response = await axiosClient.get(`/episodes/project/${args.projectId}`);
          return response.data;
        }
        case "create_episode": {
          const body = {
            projectId: args.projectId,
            episodeNumber: args.episodeNumber,
            title: args.title,
          };
          if (args.description) body.description = args.description;
          const response = await axiosClient.post("/episodes", body);
          return response.data;
        }
        default:
          return { error: `Unknown tool: ${name}` };
      }
    } catch (err) {
      const backendData = err.response?.data;
      return { error: backendData?.error || backendData?.reason || `The ${name} request failed.`, status: err.response?.status };
    }
  };

  const handleServerMessage = async (event) => {
    const msg = JSON.parse(event.data);

    switch (msg.type) {
      case "session.ready":
        readyRef.current = true;
        setStatus("ready");
        break;
      case "input.speech.started":
        setStatus("ready");
        break;
      case "reply.audio":
        setStatus("speaking");
        playAgentAudio(msg.data);
        break;
      case "reply.done":
        setStatus("ready");
        playbackTimeRef.current = audioCtxRef.current ? audioCtxRef.current.currentTime : 0;
        if (pendingToolsRef.current.length > 0) {
          for (const pending of pendingToolsRef.current) {
            wsRef.current?.send(
              JSON.stringify({ type: "tool.result", call_id: pending.call_id, result: JSON.stringify(pending.result) })
            );
          }
          pendingToolsRef.current = [];
        }
        break;
      case "transcript.user":
        appendTranscript("You", msg.text);
        break;
      case "transcript.agent":
        appendTranscript("Aozene", msg.text);
        break;
      case "tool.call": {
        const result = await executeTool(msg.name, msg.arguments || {});

        if (wsRef.current?.readyState === WebSocket.OPEN) {
          wsRef.current.send(
            JSON.stringify({
              type: "tool.result",
              call_id: msg.call_id,
              result: JSON.stringify(result),
            })
          );
        }

        break;

      }
      case "session.error":
      case "error":
        setStatus("error");
        setErrorMessage(msg.message || "An unknown error occurred.");
        break;
      case "session.ended":
        cleanup();
        setStatus("ended");
        break;
      default:
        break;
    }
  };

  const cleanup = () => {
    wsRef.current?.close();
    wsRef.current = null;
    streamRef.current?.getTracks().forEach((t) => t.stop());
    streamRef.current = null;
    audioCtxRef.current?.close();
    audioCtxRef.current = null;
    readyRef.current = false;
  };

  const startConversation = async () => {
    setErrorMessage("");
    setStatus("connecting");

    try {
      const { data } = await axiosClient.get("/voice/token");
      const token = data.token;

      const audioCtx = new AudioContext({ sampleRate: 24000 });
      audioCtxRef.current = audioCtx;
      await audioCtx.resume();
      await audioCtx.audioWorklet.addModule("/pcm-processor.js");

      const stream = await navigator.mediaDevices.getUserMedia({
        audio: { echoCancellation: true, noiseSuppression: false, sampleRate: 24000 },
      });
      streamRef.current = stream;

      const source = audioCtx.createMediaStreamSource(stream);
      const worklet = new AudioWorkletNode(audioCtx, "pcm-processor");

      worklet.port.onmessage = (e) => {
        if (readyRef.current && wsRef.current?.readyState === WebSocket.OPEN) {
          const b64 = btoa(String.fromCharCode(...new Uint8Array(e.data)));
          wsRef.current.send(JSON.stringify({ type: "input.audio", audio: b64 }));
        }
      };
      source.connect(worklet).connect(audioCtx.destination);

      const wsUrl = new URL("wss://agents.assemblyai.com/v1/ws");
      wsUrl.searchParams.set("token", token);
      const ws = new WebSocket(wsUrl);
      wsRef.current = ws;

      ws.addEventListener("open", () => {
        ws.send(
          JSON.stringify({
            type: "session.update",
            session: { system_prompt: SYSTEM_PROMPT, greeting: GREETING, output: { voice: "anna" }, tools: TOOLS },
          })
        );
      });

      ws.addEventListener("message", handleServerMessage);
      ws.addEventListener("close", () => {
        setStatus((prevStatus) => (prevStatus === "error" ? prevStatus : "ended"));
      });
    } catch (err) {
      setStatus("error");
      setErrorMessage(err.message || "Could not start the voice session.");
      cleanup();
    }
  };

  const endConversation = () => {
    if (wsRef.current && wsRef.current.readyState === WebSocket.OPEN) {
      wsRef.current.send(JSON.stringify({ type: "session.end" }));
    } else {
      cleanup();
      setStatus("idle");
    }
    // Transcript is intentionally left untouched here too.
  };

  const isActive = status === "connecting" || status === "ready" || status === "speaking";
  const isListeningOrSpeaking = status === "ready" || status === "speaking";

  return (
  <div className="va-page">
    <div className="va-grid">

      {/* LEFT: VOICE PANEL */}
      <div className="va-panel">

        <h1 className="va-heading">
          Talk to your studio.
        </h1>

        <p className="va-subheading">
          Ask Aozene about projects, artists, sessions, deadlines, and production.
        </p>

        {/* VOICE ORB */}
        <div className="va-orb-wrap">
          <div
            className={`va-orb ${
              isListeningOrSpeaking ? "va-orb--active" : ""
            } ${
              status === "speaking" ? "va-orb--speaking" : ""
            }`}
          />

          <p className="va-orb-state">
            {STATE_TEXT[status] || status}
          </p>
        </div>

        {/* CONTROLS */}
        <div className="va-controls">
          <button
            className="va-btn va-btn--primary"
            onClick={startConversation}
            disabled={isActive}
          >
            {isActive ? "Listening…" : "🎙 Start"}
          </button>

          <button
            className="va-btn va-btn--danger"
            onClick={endConversation}
            disabled={!isActive}
          >
            ■ Stop
          </button>
        </div>

        {/* ERROR */}
        {errorMessage && (
          <p className="va-error">
            {errorMessage}
          </p>
        )}

        {/* SUGGESTIONS */}
        <div className="va-suggestions">

          <div className="va-suggestions-header">
            <p className="va-suggestions__label">
              TRY SAYING
            </p>

            <span className="va-suggestions__hint">
              Quick actions
            </span>
          </div>

          <div className="va-chips">
            {SUGGESTIONS.map((s) => (
              <button
                key={s}
                type="button"
                className="va-chip"
              >
                {s}
              </button>
            ))}
          </div>

        </div>
      </div>


      {/* RIGHT: CONVERSATION PANEL */}
      <div className="va-transcript-wrap">

        {/* HEADER */}
        <div className="va-transcript-header">

          <div className="va-transcript-title">
            <span className="va-transcript-header__label">
              Conversation
            </span>

            <span className="va-transcript-status">
              {isActive ? "Live" : "Ready"}
            </span>
          </div>

          {transcript.length > 0 && (
            <button
              className="va-clear-btn"
              onClick={clearConversation}
              type="button"
            >
              Clear conversation
            </button>
          )}

        </div>


        {/* CHAT */}
        <div className="va-transcript">

          {transcript.length === 0 ? (
            <div className="va-transcript-empty">
              <div className="va-transcript-empty-icon">
                🎙
              </div>

              <p className="va-transcript__empty">
                Your conversation will appear here.
              </p>

              <span className="va-transcript-empty-hint">
                Start a conversation with Aozene.
              </span>
            </div>
          ) : (
            transcript.map((entry, idx) => {

              const isUser = entry.speaker === "You";

              return (
                <div
                  key={idx}
                  className={`va-msg-row ${
                    isUser ? "va-msg-row--user" : ""
                  }`}
                >
                  <div
                    className={`va-bubble ${
                      isUser ? "va-bubble--user" : ""
                    }`}
                  >

                    <p className="va-bubble__label">
                      {isUser ? "YOU" : "DUBPILOT"}
                    </p>

                    <p className="va-bubble__text">
                      {entry.text}
                    </p>

                  </div>
                </div>
              );
            })
          )}

        </div>
      </div>

    </div>
  </div>
);
}
export default VoiceAgent;