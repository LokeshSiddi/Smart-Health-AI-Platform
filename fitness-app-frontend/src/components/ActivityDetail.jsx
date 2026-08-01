import React, { useEffect, useState } from "react";
import { useParams } from "react-router";
import { getActivityDetail } from "../services/api";
import { Box, Card, CardContent, Divider, Typography } from '@mui/material';

const ActivityDetail = () => {
    const { id } = useParams();
    const [activity, setActivity] = useState(null);
    const [recommendation, setRecommendation] = useState(null);
    
    useEffect(() => {
        let isMounted = true;
        const fetchWithRetry = async (retries = 5, delay = 2000) => {
            try {
                const response = await getActivityDetail(id);
                if(isMounted) {
                    setActivity(response.data);
                    setRecommendation(response.data.recommendation);
                }
            } catch (error) {
                if (retries > 0 && isMounted) {
                    setTimeout(() => fetchWithRetry(retries - 1, delay), delay);
                } else {
                    console.error("Failed to load recommendation after retries", error);
                }
            }
        };
       
        fetchWithRetry();
        return () => { isMounted = false; };
    }, [id]);

    if (!activity) {
        return <Typography>Loading...</Typography>
    }
    return (
        <Box sx={{maxWidth: 800, mx: 'auto', p: 2}}>
            <Card sx={{ mb: 2}}>
                <CardContent>
                    <Typography variant="h5" gutterBottom>Activity Details</Typography>
                    <Typography>Type: {activity.activityType || activity.type || 'N/A'}</Typography>
                    <Typography>Duration: {activity.duration ? `${activity.duration} minutes` : 'Not recorded in AI summary'}</Typography>
                    <Typography>Calories Burned: {activity.caloriesBurned || 'Not recorded in AI summary'}</Typography>
                    <Typography>Date: {activity.createdAt ? new Date(activity.createdAt).toLocaleString() : 'N/A'}</Typography>
                </CardContent>
            </Card>

            {recommendation && (
                <Card>
                    <CardContent>
                        <Typography variant="h5" gutterBottom>AI Recommendation</Typography>
                        <Typography variant="h6">Analysis</Typography>
                        <Typography paragraph>{activity.recommendation}</Typography>

                        <Divider sx={{ my: 2}} />

                        <Typography variant="h6">Improvements</Typography>
                        {activity?.improvements?.map((improvement, index) => (
                            <Typography key={index} paragraph>• {improvement}</Typography>
                        ))}

                        <Divider sx={{ my: 2}} />

                        <Typography variant="h6">Suggestions</Typography>
                        {activity?.suggestions?.map((suggestion, index) => (
                            <Typography key={index} paragraph>• {suggestion}</Typography>
                        ))}

                        <Divider sx={{ my: 2}} />

                        <Typography variant="h6">Safety Guidelines</Typography>
                        {activity?.safety?.map((safety, index) => (
                            <Typography key={index} paragraph>• {safety}</Typography>
                        ))}

                    </CardContent>
                </Card>
            )}

        </Box>
    )
}

export default ActivityDetail