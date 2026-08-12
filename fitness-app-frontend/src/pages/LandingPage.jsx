import { Box, Button, Typography, Container, Paper, Grid } from '@mui/material';
import FitnessCenter from '@mui/icons-material/FitnessCenter';
import Insights from '@mui/icons-material/Insights';
import Timeline from '@mui/icons-material/Timeline';
import Psychology from '@mui/icons-material/Psychology';

const features = [
  {
    icon: <Timeline sx={{ fontSize: 32 }} />,
    title: 'Track Activities',
    desc: 'Log workouts and monitor progress over time.',
  },
  {
    icon: <Psychology sx={{ fontSize: 32 }} />,
    title: 'AI Recommendations',
    desc: 'Get personalized insights powered by AI.',
  },
  {
    icon: <Insights sx={{ fontSize: 32 }} />,
    title: 'Rich Metrics',
    desc: 'Visualize calories, duration, and performance trends.',
  },
];

const LandingPage = ({ onLogin }) => {
  return (
    <Box
      sx={{
        minHeight: '100vh',
        background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
        display: 'flex',
        alignItems: 'center',
        py: 6,
      }}
    >
      <Container maxWidth="md">
        <Paper
          elevation={8}
          sx={{
            p: { xs: 4, md: 6 },
            borderRadius: 4,
            textAlign: 'center',
          }}
        >
          <Box
            sx={{
              display: 'inline-flex',
              bgcolor: 'primary.main',
              color: 'white',
              p: 2,
              borderRadius: 3,
              mb: 2,
            }}
          >
            <FitnessCenter sx={{ fontSize: 40 }} />
          </Box>
          <Typography variant="h3" sx={{ fontWeight: 800, mb: 1 }}>
            Smart Fit AI
          </Typography>
          <Typography variant="h6" color="text.secondary" sx={{ mb: 4 }}>
            Your intelligent fitness companion
          </Typography>

          <Grid container spacing={3} sx={{ mb: 4 }}>
            {features.map((f, i) => (
              <Grid item xs={12} sm={4} key={i}>
                <Box sx={{ textAlign: 'center', p: 2 }}>
                  <Box sx={{ color: 'primary.main', mb: 1 }}>{f.icon}</Box>
                  <Typography variant="subtitle1" sx={{ fontWeight: 700 }}>
                    {f.title}
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    {f.desc}
                  </Typography>
                </Box>
              </Grid>
            ))}
          </Grid>

          <Button
            variant="contained"
            size="large"
            onClick={onLogin}
            sx={{ px: 6, py: 1.5, fontSize: '1rem' }}
          >
            Get Started
          </Button>
        </Paper>
      </Container>
    </Box>
  );
};

export default LandingPage;