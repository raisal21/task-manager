-- dec_07: the database also rejects a name with only spaces or tabs. A backstop for the rule in BoardRules.
ALTER TABLE boards
    ADD CONSTRAINT boards_name_not_blank CHECK (name ~ '\S');
