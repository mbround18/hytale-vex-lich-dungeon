import React from "react";
import "./styles/app.scss";

/**
 * AppProvider wraps the application with shared styling and configuration.
 * Place this at the root of your app to ensure consistent design system application.
 *
 * Usage:
 * ```tsx
 * ReactDOM.createRoot(document.getElementById('root')!).render(
 *   <React.StrictMode>
 *     <AppProvider>
 *       <App />
 *     </AppProvider>
 *   </React.StrictMode>
 * );
 * ```
 */
export function AppProvider({ children }: { children: React.ReactNode }) {
  return <>{children}</>;
}

export default AppProvider;
