const express = require('express');
const crypto = require('crypto');
const app = express();
app.use(express.json());

const port = process.env.PORT || 3000;
const BOT_SECRET = "BOT_SECRET_123"; 

const keys = new Map(); 

function generateKey() {
    return "ZANI-" + crypto.randomBytes(6).toString('hex').toUpperCase();
}

app.post('/api/generate', (req, res) => {
    const { userId, days } = req.body;
    if (req.headers['x-bot-token'] !== BOT_SECRET) {
        return res.status(403).json({ error: 'Forbidden' });
    }
    
    const key = generateKey();
    const expires = days === 0 ? Infinity : Date.now() + (days * 24 * 60 * 60 * 1000);
    keys.set(key, { expires });
    res.json({ key, expires });
});

app.post('/api/validate', (req, res) => {
    const { key } = req.body;
    if (!keys.has(key)) return res.json({ status: 'invalid' });
    const data = keys.get(key);
    if (data.expires !== Infinity && Date.now() > data.expires) {
        keys.delete(key);
        return res.json({ status: 'expired' });
    }
    res.json({ status: 'valid', expires: data.expires });
});

app.get('/api/offsets', (req, res) => {
    const key = req.headers['x-auth-key'];
    if (!keys.has(key)) return res.status(403).json({ error: 'Invalid key' });
    res.json({ localPlayer: 0x00A1B2C3, entityList: 0x00D4E5F6 });
});

app.listen(port, () => console.log(`Server on ${port}`));
