import ArrowBackRoundedIcon from "@mui/icons-material/ArrowBackRounded";
import {
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  Divider,
  Grid,
  Stack,
  Typography,
} from "@mui/material";
import { useNavigate } from "react-router-dom";

function ActivityDetail({ activity }) {
  const navigate = useNavigate();

  const metrics = Object.entries(activity.additionalMetrics || {});

  return (
    <Stack spacing={3}>
      <Button
        startIcon={<ArrowBackRoundedIcon />}
        onClick={() => navigate("/activities")}
        sx={{ alignSelf: "flex-start" }}
      >
        Back to activities
      </Button>

      <Card>
        <CardContent>
          <Typography variant="h4" gutterBottom>
            {activity.type}
          </Typography>

          <Typography color="text.secondary" sx={{ mb: 3 }}>
            {new Date(activity.createdAt).toLocaleString()}
          </Typography>

          <Grid container spacing={2}>
            <Grid item xs={12} sm={4}>
              <Chip
                label={`${activity.duration} minutes`}
                color="primary"
                sx={{ width: "100%" }}
              />
            </Grid>

            <Grid item xs={12} sm={4}>
              <Chip
                label={`${activity.caloriesBurned} kcal`}
                color="secondary"
                sx={{ width: "100%" }}
              />
            </Grid>

            <Grid item xs={12} sm={4}>
              <Chip
                label={`${metrics.length} metrics`}
                sx={{ width: "100%" }}
              />
            </Grid>
          </Grid>
        </CardContent>
      </Card>

      <Card>
        <CardContent>
          <Typography variant="h6" gutterBottom>
            Additional metrics
          </Typography>

          <Divider sx={{ mb: 2 }} />

          {metrics.length === 0 ? (
            <Typography color="text.secondary">
              No additional metrics were recorded.
            </Typography>
          ) : (
            <Stack spacing={1.5}>
              {metrics.map(([key, value]) => (
                <Stack
                  key={key}
                  direction="row"
                  justifyContent="space-between"
                  sx={{
                    p: 1.5,
                    borderRadius: 2,
                    bgcolor: "rgba(148,163,184,.08)",
                  }}
                >
                  <Typography color="text.secondary">{key}</Typography>
                  <Typography fontWeight={700}>{String(value)}</Typography>
                </Stack>
              ))}
            </Stack>
          )}
        </CardContent>
      </Card>

      {/* Render your recommendation card here */}
    </Stack>
  );
}

export default ActivityDetail;