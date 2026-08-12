import {
  Card,
  CardContent,
  CardActionArea,
  Grid,
  Typography,
  Box,
  Chip,
  CircularProgress,
  Alert,
} from '@mui/material';
import LocalFireDepartment from '@mui/icons-material/LocalFireDepartment';
import Timer from '@mui/icons-material/Timer';
import ArrowForwardIos from '@mui/icons-material/ArrowForwardIos';
import FitnessCenter from '@mui/icons-material/FitnessCenter';
import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { getActivities } from '../services/api';

const ActivityList = () => {
  const [activities, setActivities] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const navigate = useNavigate();

  const fetchActivities = async () => {
    try {
      setLoading(true);
      const response = await getActivities();
      setActivities(response.data || []);
    } catch (err) {
      console.error(err);
      setError('Failed to load activities.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchActivities();
  }, []);

  const getActivityLabel = (a) => {
    if (a.type === 'OTHER' && a.additionalMetrics?.customType) {
      return a.additionalMetrics.customType;
    }
    return a.type?.replace('_', ' ');
  };

  return (
    <Card>
      <CardContent>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 2 }}>
          <FitnessCenter color="primary" />
          <Typography variant="h6" sx={{ fontWeight: 700 }}>
            Your Activities
          </Typography>
          <Chip
            label={activities.length}
            size="small"
            color="primary"
            sx={{ ml: 'auto' }}
          />
        </Box>

        {loading ? (
          <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
            <CircularProgress />
          </Box>
        ) : error ? (
          <Alert severity="error">{error}</Alert>
        ) : activities.length === 0 ? (
          <Box sx={{ py: 4, textAlign: 'center' }}>
            <Typography color="text.secondary">
              No activities yet. Add your first activity!
            </Typography>
          </Box>
        ) : (
          <Grid container spacing={2}>
            {activities.map((activity) => (
              <Grid item xs={12} key={activity.id}>
                <Card variant="outlined" sx={{ '&:hover': { borderColor: 'primary.main' } }}>
                  <CardActionArea onClick={() => navigate(`/activities/${activity.id}`)}>
                    <CardContent>
                      <Box
                        sx={{
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'space-between',
                        }}
                      >
                        <Box>
                          <Chip
                            label={getActivityLabel(activity)}
                            color="primary"
                            size="small"
                            sx={{ mb: 1, fontWeight: 600 }}
                          />
                          <Box sx={{ display: 'flex', gap: 3, mt: 1, flexWrap: 'wrap' }}>
                            <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
                              <Timer fontSize="small" color="warning" />
                              <Typography variant="body2">
                                <b>{activity.duration}</b> min
                              </Typography>
                            </Box>
                            <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
                              <LocalFireDepartment fontSize="small" color="error" />
                              <Typography variant="body2">
                                <b>{activity.caloriesBurned}</b> kcal
                              </Typography>
                            </Box>
                          </Box>
                        </Box>
                        <ArrowForwardIos fontSize="small" color="action" />
                      </Box>
                    </CardContent>
                  </CardActionArea>
                </Card>
              </Grid>
            ))}
          </Grid>
        )}
      </CardContent>
    </Card>
  );
};

export default ActivityList;