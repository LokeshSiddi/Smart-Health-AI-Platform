import { useEffect, useState } from 'react';
import { Container, Grid, Box, Typography, Paper } from '@mui/material';
import FitnessCenter from '@mui/icons-material/FitnessCenter';
import LocalFireDepartment from '@mui/icons-material/LocalFireDepartment';
import Timer from '@mui/icons-material/Timer';
import TrendingUp from '@mui/icons-material/TrendingUp';
import ActivityForm from '../components/ActivityForm';
import ActivityList from '../components/ActivityList';
import { getActivities } from '../services/api';

const StatCard = ({ icon, label, value, color }) => (
  <Paper
    sx={{
      p: 2.5,
      display: 'flex',
      alignItems: 'center',
      gap: 2,
      height: '100%',
    }}
  >
    <Box
      sx={{
        bgcolor: `${color}.light`,
        color: `${color}.dark`,
        p: 1.5,
        borderRadius: 2,
        display: 'flex',
      }}
    >
      {icon}
    </Box>
    <Box>
      <Typography variant="caption" color="text.secondary">
        {label}
      </Typography>
      <Typography variant="h5" sx={{ fontWeight: 700 }}>
        {value}
      </Typography>
    </Box>
  </Paper>
);

const Dashboard = () => {
  const [refreshKey, setRefreshKey] = useState(0);
  const [stats, setStats] = useState({
    total: 0,
    totalCalories: 0,
    totalDuration: 0,
    avgDuration: 0,
  });

  const handleActivityAdded = () => setRefreshKey((k) => k + 1);

  useEffect(() => {
    (async () => {
      try {
        const res = await getActivities();
        const list = res.data || [];
        const totalCalories = list.reduce(
          (s, a) => s + (Number(a.caloriesBurned) || 0),
          0
        );
        const totalDuration = list.reduce(
          (s, a) => s + (Number(a.duration) || 0),
          0
        );
        setStats({
          total: list.length,
          totalCalories,
          totalDuration,
          avgDuration: list.length ? Math.round(totalDuration / list.length) : 0,
        });
      } catch (e) {
        console.error(e);
      }
    })();
  }, [refreshKey]);

  return (
    <Container maxWidth="lg">
      <Box sx={{ mb: 4 }}>
        <Typography variant="h4" sx={{ fontWeight: 800, mb: 0.5 }}>
          Dashboard
        </Typography>
        <Typography color="text.secondary">
          Track your fitness journey with AI-powered insights
        </Typography>
      </Box>

      {/* Stats */}
      <Grid container spacing={2} sx={{ mb: 4 }}>
        <Grid item xs={12} sm={6} md={3}>
          <StatCard
            icon={<FitnessCenter />}
            label="Total Activities"
            value={stats.total}
            color="primary"
          />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <StatCard
            icon={<LocalFireDepartment />}
            label="Calories Burned"
            value={stats.totalCalories}
            color="error"
          />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <StatCard
            icon={<Timer />}
            label="Total Minutes"
            value={stats.totalDuration}
            color="warning"
          />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <StatCard
            icon={<TrendingUp />}
            label="Avg Duration"
            value={`${stats.avgDuration} min`}
            color="success"
          />
        </Grid>
      </Grid>

      <Grid container spacing={3}>
        <Grid item xs={12} md={5}>
          <ActivityForm onActivityAdded={handleActivityAdded} />
        </Grid>
        <Grid item xs={12} md={7}>
          <ActivityList key={refreshKey} />
        </Grid>
      </Grid>
    </Container>
  );
};

export default Dashboard;