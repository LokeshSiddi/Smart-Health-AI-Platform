import { Box, CircularProgress } from "@mui/material";
import { useContext, useEffect, useState } from "react";
import { AuthContext } from "react-oauth2-code-pkce";
import { useDispatch } from "react-redux";
import { BrowserRouter as Router, Navigate, Route, Routes } from "react-router-dom";
import { setCredentials } from "./store/authSlice";
import LandingPage from "./pages/LandingPage";
import Navbar from "./components/Navbar";
import Dashboard from "./pages/Dashboard";
import ActivityDetailPage from "./pages/ActivityDetailPage";

// const token = "mock-jwt-token-12345";
// const tokenData = { sub: "mock-user-id-999", name: "Test User" };

function App() {
  const { token, tokenData, logIn, logOut } = useContext(AuthContext);
  
  // MOCK AUTHENTICATION: 
  // const logIn = () => console.log("Mock Login triggered");
  // const logOut = () => console.log("Mock Logout triggered");
  const isAuthenticated = true;

  const dispatch = useDispatch();
  const [authReady, setAuthReady] = useState(false);
  
  useEffect(() => {
    if (token) {
      dispatch(setCredentials({token, user: tokenData}));
      setAuthReady(true);
    }
  }, [token, tokenData, dispatch]);

  if (!token) {
    return <LandingPage onLogin={logIn} />;
  }

  return (
    <Router>
      <Box sx={{ minHeight: '100vh', bgcolor: 'background.default' }}>
        <Navbar user={tokenData} onLogout={logOut} />
        <Box sx={{ py: 3 }}>
          <Routes>
            <Route path="/dashboard" element={<Dashboard />} />
            <Route path="/activities/:id" element={<ActivityDetailPage />} />
            <Route path="/" element={<Navigate to="/dashboard" replace />} />
            <Route path="*" element={<Navigate to="/dashboard" replace />} />
          </Routes>
        </Box>
      </Box>
    </Router>
  );
}

export default App;