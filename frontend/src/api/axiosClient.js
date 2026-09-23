import axios from "axios";

const axiosClient = axios.create({
  baseURL: "https://aozene-ai.onrender.com/api",
  headers: {
    "Content-Type": "application/json",
  },
});

export default axiosClient;