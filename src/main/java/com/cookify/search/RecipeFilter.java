package com.cookify.search;

/**
 * Optional, multi-dimensional search/filter criteria for {@link RecipeSearchService#search}.
 * Every field is nullable/optional — a null field means "don't filter on this dimension".
 * Bound directly from the Explore page's query string.
 */
public class RecipeFilter {

    /** Matches recipe name, creator username, or dietary type ("veg"/"non-veg"). */
    private String query;
    private Boolean veg;
    private Integer maxCost;
    private Integer maxCookingTime;
    private Integer minSpeed;
    private Integer minDifficulty;
    private Integer maxCalories;
    private String ingredient;
    /** One of {@code popular|newest|rating}; defaults to {@code popular} when blank. */
    private String sort;

    /**
     * Explore page "search in" scope checkboxes. Each is {@code null} when its checkbox was not
     * submitted (unchecked boxes don't appear in the query string at all — including on first
     * visit, before any form submission). {@link #isDefaultSearchScope()} treats "all three null"
     * as "match any field", preserving the original (pre-checkbox) behavior.
     */
    private Boolean searchInTitle;
    private Boolean searchInCreator;
    private Boolean searchInType;

    public String getQuery() { return query; }
    public void setQuery(String v) { this.query = v; }
    public Boolean getVeg() { return veg; }
    public void setVeg(Boolean v) { this.veg = v; }
    public Integer getMaxCost() { return maxCost; }
    public void setMaxCost(Integer v) { this.maxCost = v; }
    public Integer getMaxCookingTime() { return maxCookingTime; }
    public void setMaxCookingTime(Integer v) { this.maxCookingTime = v; }
    public Integer getMinSpeed() { return minSpeed; }
    public void setMinSpeed(Integer v) { this.minSpeed = v; }
    public Integer getMinDifficulty() { return minDifficulty; }
    public void setMinDifficulty(Integer v) { this.minDifficulty = v; }
    public Integer getMaxCalories() { return maxCalories; }
    public void setMaxCalories(Integer v) { this.maxCalories = v; }
    public String getIngredient() { return ingredient; }
    public void setIngredient(String v) { this.ingredient = v; }
    public String getSort() { return sort; }
    public void setSort(String v) { this.sort = v; }

    public Boolean getSearchInTitle() { return searchInTitle; }
    public void setSearchInTitle(Boolean v) { this.searchInTitle = v; }
    public Boolean getSearchInCreator() { return searchInCreator; }
    public void setSearchInCreator(Boolean v) { this.searchInCreator = v; }
    public Boolean getSearchInType() { return searchInType; }
    public void setSearchInType(Boolean v) { this.searchInType = v; }

    /** True when none of the three scope checkboxes were submitted (first visit, or every box
     * unchecked — indistinguishable in plain HTML form submission). In that case the query
     * matches any of title/creator/type, matching the pre-checkbox behavior. */
    public boolean isDefaultSearchScope() {
        return searchInTitle == null && searchInCreator == null && searchInType == null;
    }

    public boolean matchTitle() { return isDefaultSearchScope() || Boolean.TRUE.equals(searchInTitle); }
    public boolean matchCreator() { return isDefaultSearchScope() || Boolean.TRUE.equals(searchInCreator); }
    public boolean matchType() { return isDefaultSearchScope() || Boolean.TRUE.equals(searchInType); }
}
