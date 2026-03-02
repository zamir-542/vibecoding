const express = require('express');
const sqlite3 = require('sqlite3').verbose();
const path = require('path');

const app = express();
const PORT = process.env.PORT || 3000;

// Database setup
const dbPath = path.join(__dirname, 'database.sqlite');
const db = new sqlite3.Database(dbPath, (err) => {
    if (err) {
        console.error('Error opening database', err.message);
    } else {
        console.log('Connected to the SQLite database.');
        db.run(`CREATE TABLE IF NOT EXISTS posts (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            title TEXT NOT NULL,
            content TEXT NOT NULL,
            created_at DATETIME DEFAULT CURRENT_TIMESTAMP
        )`, (err) => {
            if (err) {
                console.error('Error creating table', err.message);
            }
        });
    }
});

app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// Serve static files from 'public' directory
app.use(express.static(path.join(__dirname, 'public')));

// Basic Auth Middleware for Admin
const basicAuth = (req, res, next) => {
    const authHeader = req.headers.authorization;
    if (!authHeader) {
        res.setHeader('WWW-Authenticate', 'Basic');
        return res.status(401).send('Authentication required.');
    }

    const auth = Buffer.from(authHeader.split(' ')[1], 'base64').toString().split(':');
    const user = auth[0];
    const pass = auth[1];

    if (user === 'admin' && pass === 'password') {
        next();
    } else {
        res.setHeader('WWW-Authenticate', 'Basic');
        return res.status(401).send('Authentication failed.');
    }
};

// Admin page Route (Protected)
app.get('/admin', basicAuth, (req, res) => {
    // Serve the admin page HTML
    res.sendFile(path.join(__dirname, 'views', 'admin.html'));
});

// Admin script and styles can just be served statically since they don't contain sensitive data,
// but we just put them in public and the admin.html references them, or inline them.
// Let's create specific routes to prevent caching or we just inline the CSS/JS for admin to be simpler.

// API Routes
// Get all posts - Public
app.get('/api/posts', (req, res) => {
    db.all(`SELECT * FROM posts ORDER BY created_at DESC`, [], (err, rows) => {
        if (err) {
            res.status(500).json({ error: err.message });
            return;
        }
        res.json(rows);
    });
});

// Create a post (Protected)
app.post('/api/posts', basicAuth, (req, res) => {
    const { title, content } = req.body;
    if (!title || !content) {
        return res.status(400).json({ error: 'Title and content are required' });
    }
    db.run(`INSERT INTO posts (title, content) VALUES (?, ?)`, [title, content], function(err) {
        if (err) {
            res.status(500).json({ error: err.message });
            return;
        }
        res.status(201).json({ id: this.lastID, title, content });
    });
});

// Update a post (Protected)
app.put('/api/posts/:id', basicAuth, (req, res) => {
    const { title, content } = req.body;
    db.run(`UPDATE posts SET title = ?, content = ? WHERE id = ?`, [title, content, req.params.id], function(err) {
        if (err) {
            res.status(500).json({ error: err.message });
            return;
        }
        res.json({ message: 'Post updated' });
    });
});

// Delete a post (Protected)
app.delete('/api/posts/:id', basicAuth, (req, res) => {
    db.run(`DELETE FROM posts WHERE id = ?`, req.params.id, function(err) {
        if (err) {
            res.status(500).json({ error: err.message });
            return;
        }
        res.json({ message: 'Post deleted' });
    });
});

app.listen(PORT, () => {
    console.log(`Server is running on http://localhost:${PORT}`);
});
