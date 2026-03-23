package dk.ufst.opendebt.common.timeline;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

/**
 * Configuration properties for role-based timeline visibility. Prefix: opendebt.timeline.visibility
 * Enable via @EnableConfigurationProperties(TimelineVisibilityProperties.class) in each portal.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "opendebt.timeline.visibility")
public class TimelineVisibilityProperties {

  /**
   * Maps role name → allowed event categories. Role names must match Spring Security role strings
   * (without ROLE_ prefix).
   */
  private Map<String, Set<EventCategory>> roleCategories = new HashMap<>();

  /**
   * Returns the set of event categories allowed for the given role. Returns an empty set if the
   * role is not configured (yields empty timeline — safe degradation).
   */
  public Set<EventCategory> getAllowedCategories(String role) {
    return roleCategories.getOrDefault(role, Set.of());
  }
}
