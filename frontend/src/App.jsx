import { BrowserRouter, Routes, Route } from "react-router-dom";
import Dashboard from "./pages/Dashboard";
import VoiceAgent from "./pages/VoiceAgent";
import ArtistConfirmation from "./pages/ArtistConfirmation";

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Dashboard />} />
        <Route path="/voice" element={<VoiceAgent />} />
        <Route path="/artist-confirmation/:sessionId" element={<ArtistConfirmation />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;