INSERT INTO cards (question, answer, detail, subject, difficulty, interval, ease_factor, repetitions, next_review_date, created_at, card_type, tags, is_favorite, phonetic, example, collocations, etymology, hint, rhyme, derivatives, mastered) VALUES
('capitalism', '资本主义', '', '经济', 2.5, 0, 2.5, 0, 0, 0, '问答', '', 0, '', '', '', '', '', '', '', 0),
('mitochondria', '线粒体', '', '生物', 2.5, 0, 2.5, 0, 0, 0, '问答', '', 0, '', '', '', '', '', '', '', 0);
SELECT id, question, next_review_date FROM cards;
