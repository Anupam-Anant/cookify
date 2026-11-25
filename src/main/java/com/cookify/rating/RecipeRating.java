package com.cookify.rating;

import jakarta.persistence.*;

/**
 * PDF-faithful {@code recipe_rating}: one row per (recipe, user). The 1-5 star value is
 * stored one-hot (rate_{n}=1, the rest 0); the same row also carries the user's single
 * free-text {@code comments} for that recipe.
 */
@Entity
@Table(name = "recipe_rating")
public class RecipeRating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rating_id")
    private Long ratingId;

    @Column(name = "recipe_id", nullable = false)
    private Long recipeId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "rate_1")
    private int rate1 = 0;

    @Column(name = "rate_2")
    private int rate2 = 0;

    @Column(name = "rate_3")
    private int rate3 = 0;

    @Column(name = "rate_4")
    private int rate4 = 0;

    @Column(name = "rate_5")
    private int rate5 = 0;

    private String comments;

    public Long getRatingId() { return ratingId; }
    public Long getRecipeId() { return recipeId; }
    public void setRecipeId(Long v) { this.recipeId = v; }
    public Long getUserId() { return userId; }
    public void setUserId(Long v) { this.userId = v; }
    public int getRate1() { return rate1; }
    public void setRate1(int v) { this.rate1 = v; }
    public int getRate2() { return rate2; }
    public void setRate2(int v) { this.rate2 = v; }
    public int getRate3() { return rate3; }
    public void setRate3(int v) { this.rate3 = v; }
    public int getRate4() { return rate4; }
    public void setRate4(int v) { this.rate4 = v; }
    public int getRate5() { return rate5; }
    public void setRate5(int v) { this.rate5 = v; }
    public String getComments() { return comments; }
    public void setComments(String v) { this.comments = v; }
}
