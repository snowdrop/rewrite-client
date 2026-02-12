INSERT INTO books(id, title, author, publicationyear, isbn) VALUES (1, 'Effective Java', 'Joshua Bloch', 2018, '978-0134685991');
INSERT INTO books(id, title, author, publicationyear, isbn) VALUES (2, 'Clean Code', 'Robert C. Martin', 2008, '978-0132350884');
INSERT INTO books(id, title, author, publicationyear, isbn) VALUES (3, 'Design Patterns', 'Gang of Four', 1994, '978-0201633610');
alter sequence books_seq restart with 4;