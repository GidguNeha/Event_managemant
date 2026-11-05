-- Register organizer@eventhub.local through the API, then promote that account:
-- UPDATE users SET role = 'ORGANIZER' WHERE email = 'organizer@eventhub.local';
-- Run this file after creating/promoting that account.
SET @organizer_id = (SELECT id FROM users WHERE email = 'organizer@eventhub.local');

INSERT INTO events
(name, description, category, event_date, start_time, venue, image_url, ticket_price, total_tickets, available_tickets, organizer_id)
VALUES
('Delhi Sufi & Folk Night', 'An evening of Hindustani classics and contemporary Sufi vocals in the heart of Delhi.', 'Music',
 DATE_ADD(CURRENT_DATE, INTERVAL 21 DAY), '19:00:00', 'Kamani Auditorium, New Delhi', NULL, 499.00, 180, 180, @organizer_id),
('Old Delhi Street Food Walk', 'Discover Chandni Chowk''s much-loved street food with a local guide.', 'Food',
 DATE_ADD(CURRENT_DATE, INTERVAL 28 DAY), '12:00:00', 'Chandni Chowk, New Delhi', NULL, 349.00, 40, 40, @organizer_id),
('Jaipur Block Printing Workshop', 'Learn traditional hand block printing and take home your own printed fabric.', 'Arts',
 DATE_ADD(CURRENT_DATE, INTERVAL 35 DAY), '14:00:00', 'Anokhi Museum of Hand Printing, Jaipur', NULL, 699.00, 30, 30, @organizer_id),
('Ahmedabad Dandiya Raas Night', 'Celebrate Navratri with an energetic evening of Garba, Dandiya, and live folk music.', 'Music',
 DATE_ADD(CURRENT_DATE, INTERVAL 14 DAY), '19:00:00', 'GMDC Ground, Ahmedabad', NULL, 399.00, 250, 250, @organizer_id),
('Delhi Diwali Lights & Food Festival', 'Enjoy festive lights, handmade crafts, and classic Diwali treats with the community.', 'Community',
 DATE_ADD(CURRENT_DATE, INTERVAL 28 DAY), '17:00:00', 'Dilli Haat, New Delhi', NULL, 499.00, 200, 200, @organizer_id),
('Ahmedabad Makar Sankranti Kite Festival', 'Welcome Uttarayan with colorful kites, Gujarati snacks, and family-friendly activities.', 'Community',
 DATE_ADD(CURRENT_DATE, INTERVAL 90 DAY), '09:00:00', 'Sabarmati Riverfront, Ahmedabad', NULL, 299.00, 150, 150, @organizer_id);
