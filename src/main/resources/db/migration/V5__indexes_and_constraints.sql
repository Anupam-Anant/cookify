-- Foreign-key and filter columns that are queried frequently but were missing a supporting
-- index: lookups by creator, by recipe/user on ratings, chat participants, subscriptions,
-- warnings, saved recipes, and the veg/non-veg filter all previously forced a full table scan.
CREATE INDEX IF NOT EXISTS idx_recipe_creator_id ON recipe(recipe_creator_id);
CREATE INDEX IF NOT EXISTS idx_recipe_rating_recipe_id ON recipe_rating(recipe_id);
CREATE INDEX IF NOT EXISTS idx_recipe_rating_user_id ON recipe_rating(user_id);
CREATE INDEX IF NOT EXISTS idx_chat_message_sender_id ON chat_message(sender_id);
CREATE INDEX IF NOT EXISTS idx_chat_message_recipient_id ON chat_message(recipient_id);
CREATE INDEX IF NOT EXISTS idx_subscription_creator_id ON subscription(creator_id);
CREATE INDEX IF NOT EXISTS idx_subscription_subscriber_id ON subscription(subscriber_id);
CREATE INDEX IF NOT EXISTS idx_warning_user_id ON warning(user_id);
CREATE INDEX IF NOT EXISTS idx_saved_recipe_user_id ON saved_recipe(user_id);
CREATE INDEX IF NOT EXISTS idx_saved_recipe_recipe_id ON saved_recipe(recipe_id);
CREATE INDEX IF NOT EXISTS idx_recipe_category_veg_nonveg ON recipe_category(veg_nonveg);

-- The app already upserts ratings via find-or-create (RatingService.rate /
-- CommentService.addComment both look up by recipe_id+user_id before inserting), so this
-- constraint only makes the existing one-row-per-(recipe,user) invariant enforceable by the
-- database itself instead of relying solely on application code.
ALTER TABLE recipe_rating ADD CONSTRAINT uq_recipe_rating_recipe_user UNIQUE (recipe_id, user_id);
