document.addEventListener('DOMContentLoaded', () => {
    fetchPosts();
});

async function fetchPosts() {
    const container = document.getElementById('posts-container');
    if (!container) return; // Only run on pages with posts-container

    try {
        const response = await fetch('/api/posts');
        const posts = await response.json();

        container.innerHTML = '';

        if (posts.length === 0) {
            container.innerHTML = '<p style="text-align: center; opacity: 0.6;">No posts yet. Check back soon!</p>';
            return;
        }

        posts.forEach(post => {
            const date = new Date(post.created_at).toLocaleDateString('en-US', {
                year: 'numeric',
                month: 'long',
                day: 'numeric'
            });

            const article = document.createElement('article');
            article.className = 'post-card';
            article.innerHTML = `
                <h3 class="post-title">${escapeHTML(post.title)}</h3>
                <time class="post-date">${date}</time>
                <div class="post-content">${escapeHTML(post.content)}</div>
            `;
            container.appendChild(article);
        });

    } catch (error) {
        console.error('Error fetching posts:', error);
        container.innerHTML = '<p style="text-align: center; color: red;">Error loading posts.</p>';
    }
}

function escapeHTML(str) {
    return str.replace(/[&<>'"]/g,
        tag => ({
            '&': '&amp;',
            '<': '&lt;',
            '>': '&gt;',
            "'": '&#39;',
            '"': '&quot;'
        }[tag])
    );
}
