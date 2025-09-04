package com.example.blog.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.*;

@Entity
@Data
@Table(name = "trunks")
public class Trunk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean publicFlag = false;

    // Owner
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id")
    @ToString.Exclude
    @JsonBackReference
    private AppUser owner;

    // Collaborators (can add/remove branches)
    @ManyToMany
    @JoinTable(
            name = "trunk_collaborators",
            joinColumns = @JoinColumn(name = "trunk_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    @JsonIgnore
    private Set<AppUser> collaborators = new HashSet<>();

    // Branches (songs)
    @OneToMany(mappedBy = "trunk", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC, id ASC")
    @JsonManagedReference
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private List<Branch> branches = new ArrayList<>();

    // Optional Spotify export linkage
    private String spotifyPlaylistId;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public boolean canView(AppUser user) {
        if (publicFlag) return true;
        if (user == null) return false;
        if (Objects.equals(owner.getId(), user.getId())) return true;
        return collaborators.stream().anyMatch(u -> Objects.equals(u.getId(), user.getId()));
    }

    public boolean canEdit(AppUser user) {
        if (user == null) return false;
        if (Objects.equals(owner.getId(), user.getId())) return true;
        return collaborators.stream().anyMatch(u -> Objects.equals(u.getId(), user.getId()));
    }

    public boolean isPublic() {
        return publicFlag;
    }

    public void setPublic(boolean isPublic) {
        this.publicFlag = isPublic;
    }
}
