package piecework.enumeration;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;

import java.io.Serializable;

/**
 * @author James Renfro
 */
public enum ActionType implements Serializable {
    ASSIGN("Assigned"), CLAIM("Claimed"), COMPLETE("Completed"), REJECT("Rejected"), SAVE("Saved"), VALIDATE("Validated"), CREATE("Created"), VIEW("Reviewed");

    private final String description;

    private ActionType(String description) {
        this.description = description;
    }

    @JsonIgnore
    public String description() {
        return description;
    }
}
