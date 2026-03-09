const express = require('express');
const { Pool } = require('pg');
const path = require('path');

const app = express();
const PORT = process.env.PORT || 3000;

// PostgreSQL connection — Railway sets DATABASE_URL automatically
const pool = new Pool({
    connectionString: process.env.DATABASE_URL,
    ssl: process.env.DATABASE_URL ? { rejectUnauthorized: false } : false,
});

// Create table on startup if it doesn't exist
pool.query(`
    CREATE TABLE IF NOT EXISTS posts (
        id SERIAL PRIMARY KEY,
        title TEXT NOT NULL,
        content TEXT NOT NULL,
        created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
    )
`).then(() => {
    console.log('Database ready.');
}).catch(err => {
    console.error('Error setting up database:', err.message);
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
    res.sendFile(path.join(__dirname, 'views', 'admin.html'));
});

// API Routes
// Get all posts - Public
app.get('/api/posts', async (req, res) => {
    try {
        const result = await pool.query('SELECT * FROM posts ORDER BY created_at DESC');
        res.json(result.rows);
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// Create a post (Protected)
app.post('/api/posts', basicAuth, async (req, res) => {
    const { title, content } = req.body;
    if (!title || !content) {
        return res.status(400).json({ error: 'Title and content are required' });
    }
    try {
        const result = await pool.query(
            'INSERT INTO posts (title, content) VALUES ($1, $2) RETURNING *',
            [title, content]
        );
        res.status(201).json(result.rows[0]);
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// Update a post (Protected)
app.put('/api/posts/:id', basicAuth, async (req, res) => {
    const { title, content } = req.body;
    try {
        await pool.query(
            'UPDATE posts SET title = $1, content = $2 WHERE id = $3',
            [title, content, req.params.id]
        );
        res.json({ message: 'Post updated' });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// Delete a post (Protected)
app.delete('/api/posts/:id', basicAuth, async (req, res) => {
    try {
        await pool.query('DELETE FROM posts WHERE id = $1', [req.params.id]);
        res.json({ message: 'Post deleted' });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

app.listen(PORT, () => {
    console.log(`Server is running on http://localhost:${PORT}`);
});
