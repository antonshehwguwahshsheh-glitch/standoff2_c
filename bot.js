const TelegramBot = require('node-telegram-bot-api');
const axios = require('axios');

const TOKEN = '9141053149:AAEIISW3XtkTFZ2eXCDXzyMTZ6WlDZyd2oU';
const ADMIN_ID = 9141053149;
const SERVER_URL = 'http://127.0.0.1:3000'; 
const BOT_SECRET = 'BOT_SECRET_123';

const bot = new TelegramBot(TOKEN, { polling: true });

// Клавиатура для обычных юзеров
const userMenu = {
    reply_markup: {
        keyboard: [
            [{ text: '👤 Профиль' }, { text: '🛒 Магазин' }]
        ],
        resize_keyboard: true
    }
};

// Клавиатура для тебя (Админа)
const adminMenu = {
    reply_markup: {
        keyboard: [
            [{ text: '👤 Профиль' }, { text: '🛒 Магазин' }],
            [{ text: '⚙️ Админка' }]
        ],
        resize_keyboard: true
    }
};

// Старт
bot.onText(/\/start/, (msg) => {
    const chatId = msg.chat.id;
    
    // Проверка на админа
    if (msg.from.id === ADMIN_ID) {
        bot.sendMessage(chatId, 'Привет, Админ! Выбери действие:', adminMenu);
    } else {
        bot.sendMessage(chatId, 'Добро пожаловать в магазин Standoff Cheat!', userMenu);
    }
});

// Обработка нажатий на кнопки
bot.on('message', async (msg) => {
    const chatId = msg.chat.id;
    const text = msg.text;
    const userId = msg.from.id;

    // Магазин
    if (text === '🛒 Магазин') {
        bot.sendMessage(chatId, 'Выбери тариф:', {
            reply_markup: {
                inline_keyboard: [
                    [{ text: '7 дней (25 ⭐)', callback_data: 'buy_7' }],
                    [{ text: '30 дней (50 ⭐)', callback_data: 'buy_30' }],
                    [{ text: '60 дней (100 ⭐)', callback_data: 'buy_60' }],
                    [{ text: 'Навсегда (200 ⭐)', callback_data: 'buy_0' }]
                ]
            }
        });
    }

    // Профиль
    if (text === '👤 Профиль') {
        bot.sendMessage(chatId, `Твой ID: ${userId}\nСтатус: Пользователь\nДля покупки ключа зайди в Магазин.`);
    }

    // Админка
    if (text === '⚙️ Админка' && userId === ADMIN_ID) {
        bot.sendMessage(chatId, 'Панель администратора:', {
            reply_markup: {
                inline_keyboard: [
                    [{ text: '🔑 Создать ключ (30 дней)', callback_data: 'admin_gen' }]
                ]
            }
        });
    }
});

// Обработка inline-кнопок
bot.on('callback_query', async (query) => {
    const chatId = query.message.chat.id;
    const data = query.data;

    if (data.startsWith('buy_')) {
        const days = parseInt(data.split('_')[1]);
        const prices = { 7: 25, 30: 50, 60: 100, 0: 200 };
        const price = prices[days];
        const title = days === 0 ? 'Навсегда' : `${days} дней`;

        bot.sendInvoice(chatId, `Чит Standoff 2 (${title})`, 'Доступ к читу', `payload_${days}`, '', 'XTR', [{ label: title, amount: price }]);
    }

    if (data === 'admin_gen' && query.from.id === ADMIN_ID) {
        try {
            const res = await axios.post(`${SERVER_URL}/api/generate`, { userId: ADMIN_ID, days: 30 }, {
                headers: { 'x-bot-token': BOT_SECRET }
            });
            bot.sendMessage(chatId, `Ключ создан: \`${res.data.key}\``, { parse_mode: 'Markdown' });
        } catch (e) {
            bot.sendMessage(chatId, 'Ошибка сервера. Проверь URL.');
        }
    }
});

// Оплата
bot.on('successful_payment', async (msg) => {
    const chatId = msg.chat.id;
    const userId = msg.from.id;
    const payload = msg.successful_payment.invoice_payload;
    const days = parseInt(payload.split('_')[1]);

    try {
        const res = await axios.post(`${SERVER_URL}/api/generate`, { userId, days }, {
            headers: { 'x-bot-token': BOT_SECRET }
        });
        bot.sendMessage(chatId, `✅ Оплата получена!\nТвой ключ: \`${res.data.key}\`\n\nСрок действия: ${days === 0 ? 'Навсегда' : days + ' дней'}\n\nСкачай лоадер: [ссылка_на_apk]`, { parse_mode: 'Markdown' });
    } catch (e) {
        bot.sendMessage(chatId, 'Ошибка выдачи ключа.');
    }
});
