package data.recent

import com.fasterxml.jackson.annotation.JsonProperty

data class  RecentProjects(
    @JsonProperty("projects")
    val projects: List<RecentProject>
)

data class RecentProject(
    @JsonProperty("name")
    val name: String,
    @JsonProperty("location")
    val location: String
    )