package com.dreamteam.breakloop_analytics.bq8;

// Aggregated focus-session numbers for one group of users (w/ pet / w/o pet).
public record Bq8Group(
        int users,                  // distinct users with at least one session
        int sessions,               // all focus sessions
        int completedSessions,      // sessions with status COMPLETED (uninterrupted)
        double completionRate,      // completedSessions
        double avgCompletedMinutes, // average duration of completed sessions, in minutes
        boolean hidden              // true users < minUsers
) {}