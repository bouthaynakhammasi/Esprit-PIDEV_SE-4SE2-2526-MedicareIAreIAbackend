package com.aziz.demosec.Entities;

/**
 * Defines the type of venue for a physical medical event.
 * This drives the automatic seating layout generation.
 * - HOTEL     → round tables with seats around each table
 * - STADIUM   → sectioned grid (sections × rows × seats)
 * - CONFERENCE → straight rows of chairs facing a stage
 */
public enum VenueType {
    HOTEL,
    STADIUM,
    CONFERENCE
}
