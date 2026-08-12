import {
  AppBar,
  Toolbar,
  Typography,
  Button,
  Box,
  Avatar,
  IconButton,
  Menu,
  MenuItem,
  Divider,
  Tooltip,
} from '@mui/material';
import {
  FitnessCenter,
  Home,
  Logout,
  AccountCircle,
} from '@mui/icons-material';
import { useNavigate, useLocation } from 'react-router-dom';
import { useState } from 'react';

const Navbar = ({ user, onLogout }) => {
  const navigate = useNavigate();
  const location = useLocation();
  const [anchorEl, setAnchorEl] = useState(null);

  const handleMenuOpen = (e) => setAnchorEl(e.currentTarget);
  const handleMenuClose = () => setAnchorEl(null);

  const displayName =
    user?.name || user?.preferred_username || user?.email || 'User';
  const initial = displayName.charAt(0).toUpperCase();

  return (
    <AppBar
      position="sticky"
      elevation={0}
      sx={{
        bgcolor: 'white',
        color: 'text.primary',
        borderBottom: '1px solid #e2e8f0',
      }}
    >
      <Toolbar sx={{ maxWidth: 1280, width: '100%', mx: 'auto' }}>
        <Box
          sx={{
            display: 'flex',
            alignItems: 'center',
            gap: 1,
            cursor: 'pointer',
            flexGrow: 1,
          }}
          onClick={() => navigate('/dashboard')}
        >
          <Box
            sx={{
              bgcolor: 'primary.main',
              color: 'white',
              p: 1,
              borderRadius: 2,
              display: 'flex',
            }}
          >
            <FitnessCenter fontSize="small" />
          </Box>
          <Typography variant="h6" sx={{ fontWeight: 700 }}>
            Smart Fit AI
          </Typography>
        </Box>

        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          <Tooltip title="Home">
            <Button
              startIcon={<Home />}
              onClick={() => navigate('/dashboard')}
              sx={{
                color: location.pathname === '/dashboard' ? 'primary.main' : 'text.secondary',
                fontWeight: location.pathname === '/dashboard' ? 700 : 500,
              }}
            >
              Home
            </Button>
          </Tooltip>

          <IconButton onClick={handleMenuOpen} sx={{ ml: 1 }}>
            <Avatar sx={{ bgcolor: 'primary.main', width: 36, height: 36 }}>
              {initial}
            </Avatar>
          </IconButton>

          <Menu
            anchorEl={anchorEl}
            open={Boolean(anchorEl)}
            onClose={handleMenuClose}
            PaperProps={{ sx: { mt: 1, minWidth: 200 } }}
          >
            <MenuItem disabled>
              <Box>
                <Typography variant="body2" sx={{ fontWeight: 600 }}>
                  {displayName}
                </Typography>
                {user?.email && (
                  <Typography variant="caption" color="text.secondary">
                    {user.email}
                  </Typography>
                )}
              </Box>
            </MenuItem>
            <Divider />
            <MenuItem
              onClick={() => {
                handleMenuClose();
                onLogout();
              }}
            >
              <Logout fontSize="small" sx={{ mr: 1 }} />
              Logout
            </MenuItem>
          </Menu>
        </Box>
      </Toolbar>
    </AppBar>
  );
};

export default Navbar;