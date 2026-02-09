import React from "react";
import ReactDOM from "react-dom/client";
import DesignSystemApp from "./layouts/DesignSystemApp";
import "./styles/app.scss";

ReactDOM.createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <DesignSystemApp />
  </React.StrictMode>,
);
