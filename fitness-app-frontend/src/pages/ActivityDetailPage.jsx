import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import {
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  CircularProgress,
  Container,
  Divider,
  Grid,
  Typography,
  Breadcrumbs,
  Link as MuiLink,
  Alert,
} from '@mui/material';
import {
  ArrowBack,
  Home,
  LocalFireDepartment,
  Timer,
  CalendarToday,
  Psychology,
  CheckCircle,
  Warning,
  Lightbulb,
  TipsAndUpdates,
} from '@mui/icons-material';
import { getActivityDetail } from '../services/api';

const InfoTile = ({ icon, label, value, color = 'primary' }) => (
  <Box
    sx={{
      display: 'flex',
      alignItems: 'center',
      gap: 2,
      p: 2,
      bgcolor: `${color}.light`,
      borderRadius: 2,
    }}
  >
    <Box sx={{ color: `${color}.dark`, display: 'flex' }}>{icon}</Box>
    <Box>
      <Typography variant="caption" color="text.secondary">
        {label}
      </Typography>
      <Typography variant="h6" sx={{ fontWeight: 700 }}>
        {value}
      </Typography>
    </Box>
  </Box>
);

const RecommendationSection = ({ icon, title, items, color }) => {
  if (!items || items.length === 0) return null;
  return (
    <Box sx={{ mb: 3 }}>
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1.5 }}>
        <Box sx={{ color: `${color}.main`, display: 'flex' }}>{icon}</Box>
        <Typography variant="h6" sx={{ fontWeight: 700 }}>
          {title}
        </Typography>
      </Box>
      <Box component="ul" sx={{ pl: 3, m: 0 }}>
        {items.map((item, i) => (
          <Typography component="li" key={i} sx={{ mb: 0.5 }}>
            {item}
          </Typography>
        ))}
      </Box>
    </Box>
  );
};

const ActivityDetailPage = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const [activity, setActivity] = useState(null);
  const [recommendation, setRecommendation] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    (async () => {
      try {
        setLoading(true);
        const res = await getActivityDetail(id);
        setActivity(res.data);
        setRecommendation(res.data.recommendation);
      } catch (err) {
        console.error(err);
        setError('Failed to load activity details.');
      } finally {
        setLoading(false);
      }
    })();
  }, [id]);

  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }}>
        <CircularProgress />
      </Box>
    );
  }

  if (error || !activity) {
    return (
      <Container maxWidth="lg">
        <Alert severity="error" sx={{ mt: 2 }}>
          {error || 'Activity not found.'}
        </Alert>
        <Button
          startIcon={<ArrowBack />}
          onClick={() => navigate('/dashboard')}
          sx={{ mt: 2 }}
        >
          Back to Dashboard
        </Button>
      </Container>
    );
  }

  return (
    <Container maxWidth="lg">
      {/* Nav row: back + breadcrumb */}
      <Box
        sx={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          mb: 3,
          flexWrap: 'wrap',
          gap: 2,
        }}
      >
        <Button
          startIcon={<ArrowBack />}
          onClick={() => navigate(-1)}
          variant="outlined"
        >
          Back
        </Button>

        <Breadcrumbs>
          <MuiLink
            component="button"
            underline="hover"
            onClick={() => navigate('/dashboard')}
            sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}
          >
            <Home fontSize="small" />
            Dashboard
          </MuiLink>
          <Typography color="text.primary">Activity Details</Typography>
        </Breadcrumbs>
      </Box>

      <Grid container spacing={3}>
        {/* Activity Details */}
        <Grid item xs={12} md={5}>
          <Card>
            <CardContent>
              <Box
                sx={{
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                  mb: 2,
                }}
              >
                <Typography variant="h5" sx={{ fontWeight: 700 }}>
                  Activity Details
                </Typography>
                <Chip
                  label={activity.type?.replace('_', ' ')}
                  color="primary"
                  sx={{ fontWeight: 600 }}
                />
              </Box>
              <Divider sx={{ mb: 3 }} />

              <Grid container spacing={2}>
                <Grid item xs={12}>
                  <InfoTile
                    icon={<Timer />}
                    label="Duration"
                    value={`${activity.duration} minutes`}
                    color="warning"
                  />
                </Grid>
                <Grid item xs={12}>
                  <InfoTile
                    icon={<LocalFireDepartment />}
                    label="Calories Burned"
                    value={`${activity.caloriesBurned} kcal`}
                    color="error"
                  />
                </Grid>
                <Grid item xs={12}>
                  <InfoTile
                    icon={<CalendarToday />}
                    label="Date"
                    value={
                      activity.createdAt
                        ? new Date(activity.createdAt).toLocaleString()
                        : 'N/A'
                    }
                    color="primary"
                  />
                </Grid>
              </Grid>
            </CardContent>
          </Card>
        </Grid>

        {/* AI Recommendation */}
        <Grid item xs={12} md={7}>
          <Card>
            <CardContent>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 2 }}>
                <Psychology color="secondary" />
                <Typography variant="h5" sx={{ fontWeight: 700 }}>
                  AI Recommendation
                </Typography>
              </Box>
              <Divider sx={{ mb: 3 }} />

              {recommendation ? (
                <>
                  <Box sx={{ mb: 3 }}>
                    <Typography
                      variant="subtitle2"
                      color="text.secondary"
                      sx={{ mb: 1 }}
                    >
                      Analysis
                    </Typography>
                    <Typography sx={{ lineHeight: 1.7 }}>
                      {recommendation}
                    </Typography>
                  </Box>

                  <Divider sx={{ my: 2 }} />

                  <RecommendationSection
                    icon={<Lightbulb />}
                    title="Improvements"
                    items={activity.improvements}
                    color="warning"
                  />

                  <RecommendationSection
                    icon={<TipsAndUpdates />}
                    title="Suggestions"
                    items={activity.suggestions}
                    color="primary"
                  />

                  <RecommendationSection
                    icon={<Warning />}
                    title="Safety"
                    items={activity.safety}
                    color="error"
                  />
                </>
              ) : (
                <Alert severity="info" icon={<CheckCircle />}>
                  AI recommendation is being generated. Please refresh in a moment.
                </Alert>
              )}
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </Container>
  );
};

export default ActivityDetailPage;