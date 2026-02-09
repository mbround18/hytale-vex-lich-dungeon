import axios from "axios";

const resolveBaseUrl = () => {
  const configured = (import.meta as any).env?.VITE_API_BASE as
    | string
    | undefined;
  if (configured) {
    return configured.replace(/\/$/, "") + "/api";
  }
  if (typeof window !== "undefined") {
    return `${window.location.origin}/api`;
  }
  return "http://localhost:3390/api";
};

const api = axios.create({
  baseURL: resolveBaseUrl(),
});

export const apiBaseUrl = api.defaults.baseURL || "/api";

export default api;
