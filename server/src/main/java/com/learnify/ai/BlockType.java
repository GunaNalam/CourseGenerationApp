package com.learnify.ai;

/**
 * The registry of recognized lesson content block types. Persistence and the frontend
 * renderer both need to agree on this set; the content array itself stays an open,
 * unvalidated JSON shape beyond the "type" field (see design/components/07... Data Model
 * Shape decision).
 */
public enum BlockType {
    HEADING,
    PARAGRAPH,
    CODE,
    VIDEO,
    MCQ;

    public static boolean isValid(String type) {
        if (type == null) {
            return false;
        }
        for (BlockType blockType : values()) {
            if (blockType.name().equalsIgnoreCase(type)) {
                return true;
            }
        }
        return false;
    }
}
