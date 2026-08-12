import { useState } from 'react';
import {
  Box,
  Button,
  FormControl,
  InputLabel,
  MenuItem,
  Select,
  TextField,
  Card,
  CardContent,
  Typography,
  Divider,
  Grid,
  Collapse,
  Alert,
  InputAdornment,
} from '@mui/material';

import AddCircleOutline from '@mui/icons-material/AddCircleOutline';
import ExpandMore from '@mui/icons-material/ExpandMore';
import ExpandLess from '@mui/icons-material/ExpandLess';
import DirectionsRun from '@mui/icons-material/DirectionsRun';
import { addActivity } from '../services/api';

const ACTIVITY_TYPES = [
  { value: 'RUNNING', label: 'Running' },
  { value: 'WALKING', label: 'Walking' },
  { value: 'CYCLING', label: 'Cycling' },
  { value: 'SWIMMING', label: 'Swimming' },
  { value: 'YOGA', label: 'Yoga' },
  { value: 'WEIGHT_TRAINING', label: 'Weight Training' },
  { value: 'HIIT', label: 'HIIT' },
  { value: 'OTHER', label: 'Other' },
];

const ActivityForm = ({ onActivityAdded }) => {
  const [activity, setActivity] = useState({
    type: 'RUNNING',
    duration: '',
    caloriesBurned: '',
    additionalMetrics: {},
  });
  const [customType, setCustomType] = useState('');
  const [showAdditional, setShowAdditional] = useState(false);
  const [metrics, setMetrics] = useState({
    distance: '',
    averageHeartRate: '',
    maxHeartRate: '',
    steps: '',
    elevationGain: '',
    notes: '',
  });
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const isOther = activity.type === 'OTHER';

  // Auto-expand additional metrics when "Other" is selected
  const handleTypeChange = (e) => {
    const val = e.target.value;
    setActivity({ ...activity, type: val });
    if (val === 'OTHER') {
      setShowAdditional(true);
    }
  };

  const handleMetricChange = (field, value) => {
    setMetrics({ ...metrics, [field]: value });
  };

  const buildPayload = () => {
    // Build additionalMetrics with non-empty values
    const additionalMetrics = {};
    Object.entries(metrics).forEach(([k, v]) => {
      if (v !== '' && v !== null && v !== undefined) {
        additionalMetrics[k] = isNaN(v) || k === 'notes' ? v : Number(v);
      }
    });

    // If OTHER, add the custom exercise name
    if (isOther && customType.trim()) {
      additionalMetrics.customType = customType.trim();
    }

    return {
      type: isOther ? 'OTHER' : activity.type,
      duration: Number(activity.duration),
      caloriesBurned: Number(activity.caloriesBurned),
      additionalMetrics,
    };
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSuccess('');

    if (isOther && !customType.trim()) {
      setError('Please specify the custom exercise name.');
      return;
    }
    if (!activity.duration || !activity.caloriesBurned) {
      setError('Duration and calories are required.');
      return;
    }

    try {
      setSubmitting(true);
      await addActivity(buildPayload());
      setSuccess('Activity added successfully!');
      setActivity({
        type: 'RUNNING',
        duration: '',
        caloriesBurned: '',
        additionalMetrics: {},
      });
      setCustomType('');
      setMetrics({
        distance: '',
        averageHeartRate: '',
        maxHeartRate: '',
        steps: '',
        elevationGain: '',
        notes: '',
      });
      setShowAdditional(false);
      onActivityAdded?.();
      setTimeout(() => setSuccess(''), 3000);
    } catch (err) {
      console.error(err);
      setError('Failed to add activity. Please try again.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Card>
      <CardContent>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 2 }}>
          <DirectionsRun color="primary" />
          <Typography variant="h6" sx={{ fontWeight: 700 }}>
            Log New Activity
          </Typography>
        </Box>
        <Divider sx={{ mb: 3 }} />

        {error && (
          <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError('')}>
            {error}
          </Alert>
        )}
        {success && (
          <Alert severity="success" sx={{ mb: 2 }}>
            {success}
          </Alert>
        )}

        <Box component="form" onSubmit={handleSubmit}>
          <FormControl fullWidth sx={{ mb: 2 }}>
            <InputLabel>Activity Type</InputLabel>
            <Select
              value={activity.type}
              label="Activity Type"
              onChange={handleTypeChange}
            >
              {ACTIVITY_TYPES.map((t) => (
                <MenuItem key={t.value} value={t.value}>
                  {t.label}
                </MenuItem>
              ))}
            </Select>
          </FormControl>

          {/* Custom type input when "Other" is chosen */}
          <Collapse in={isOther}>
            <TextField
              fullWidth
              required={isOther}
              label="Custom Exercise Name"
              placeholder="e.g., Swimming, Boxing, Rock Climbing"
              value={customType}
              onChange={(e) => setCustomType(e.target.value)}
              sx={{ mb: 2 }}
              helperText="Specify the type of exercise you performed"
            />
          </Collapse>

          <Grid container spacing={2}>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                required
                label="Duration"
                type="number"
                value={activity.duration}
                onChange={(e) =>
                  setActivity({ ...activity, duration: e.target.value })
                }
                InputProps={{
                  endAdornment: <InputAdornment position="end">min</InputAdornment>,
                }}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                required
                label="Calories Burned"
                type="number"
                value={activity.caloriesBurned}
                onChange={(e) =>
                  setActivity({ ...activity, caloriesBurned: e.target.value })
                }
                InputProps={{
                  endAdornment: <InputAdornment position="end">kcal</InputAdornment>,
                }}
              />
            </Grid>
          </Grid>

          {/* Additional metrics toggle */}
          <Button
            fullWidth
            onClick={() => setShowAdditional((s) => !s)}
            endIcon={showAdditional ? <ExpandLess /> : <ExpandMore />}
            sx={{ mt: 2, justifyContent: 'space-between' }}
          >
            Additional Metrics (Optional)
          </Button>

          <Collapse in={showAdditional}>
            <Box sx={{ mt: 2, p: 2, bgcolor: 'grey.50', borderRadius: 2 }}>
              <Grid container spacing={2}>
                <Grid item xs={12} sm={6}>
                  <TextField
                    fullWidth
                    size="small"
                    label="Distance"
                    type="number"
                    value={metrics.distance}
                    onChange={(e) => handleMetricChange('distance', e.target.value)}
                    InputProps={{
                      endAdornment: <InputAdornment position="end">km</InputAdornment>,
                    }}
                  />
                </Grid>
                <Grid item xs={12} sm={6}>
                  <TextField
                    fullWidth
                    size="small"
                    label="Steps"
                    type="number"
                    value={metrics.steps}
                    onChange={(e) => handleMetricChange('steps', e.target.value)}
                  />
                </Grid>
                <Grid item xs={12} sm={6}>
                  <TextField
                    fullWidth
                    size="small"
                    label="Avg Heart Rate"
                    type="number"
                    value={metrics.averageHeartRate}
                    onChange={(e) =>
                      handleMetricChange('averageHeartRate', e.target.value)
                    }
                    InputProps={{
                      endAdornment: <InputAdornment position="end">bpm</InputAdornment>,
                    }}
                  />
                </Grid>
                <Grid item xs={12} sm={6}>
                  <TextField
                    fullWidth
                    size="small"
                    label="Max Heart Rate"
                    type="number"
                    value={metrics.maxHeartRate}
                    onChange={(e) =>
                      handleMetricChange('maxHeartRate', e.target.value)
                    }
                    InputProps={{
                      endAdornment: <InputAdornment position="end">bpm</InputAdornment>,
                    }}
                  />
                </Grid>
                <Grid item xs={12} sm={6}>
                  <TextField
                    fullWidth
                    size="small"
                    label="Elevation Gain"
                    type="number"
                    value={metrics.elevationGain}
                    onChange={(e) =>
                      handleMetricChange('elevationGain', e.target.value)
                    }
                    InputProps={{
                      endAdornment: <InputAdornment position="end">m</InputAdornment>,
                    }}
                  />
                </Grid>
                <Grid item xs={12}>
                  <TextField
                    fullWidth
                    size="small"
                    label="Notes"
                    multiline
                    rows={2}
                    value={metrics.notes}
                    onChange={(e) => handleMetricChange('notes', e.target.value)}
                    placeholder="Any additional details..."
                  />
                </Grid>
              </Grid>
            </Box>
          </Collapse>

          <Button
            type="submit"
            variant="contained"
            fullWidth
            size="large"
            disabled={submitting}
            startIcon={<AddCircleOutline />}
            sx={{ mt: 3 }}
          >
            {submitting ? 'Adding...' : 'Add Activity'}
          </Button>
        </Box>
      </CardContent>
    </Card>
  );
};

export default ActivityForm;