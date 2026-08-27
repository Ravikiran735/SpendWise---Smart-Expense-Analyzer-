/**
 * SpendWise — Smart Transaction Categorizer & Confidence Engine
 * Shared categorization dictionary matching Android ImportAnalyzer.kt
 */

const TransactionCategorizer = {
    /**
     * Categorize transaction based on description and initial type
     * Returns { category: string, confidence: number, type: string }
     */
    categorize(description, currentType = 'Expense') {
        const d = (description || '').toLowerCase();

        // 1. Income Checks
        if (this.containsAny(d, ['salary', 'payroll', 'stipend', 'wages', 'direct dep'])) {
            return { category: 'Salary', confidence: 0.98, type: 'Income' };
        }
        if (this.containsAny(d, ['freelance', 'upwork', 'fiverr', 'client payment', 'consulting'])) {
            return { category: 'Freelance', confidence: 0.95, type: 'Income' };
        }
        if (this.containsAny(d, ['business', 'merchant payout', 'stripe payout', 'razorpay', 'revenue'])) {
            return { category: 'Business', confidence: 0.92, type: 'Income' };
        }
        if (this.containsAny(d, ['cashback', 'reward', 'gift', 'bonus', 'refund'])) {
            return { category: 'Gift', confidence: 0.88, type: 'Income' };
        }
        if (this.containsAny(d, ['dividend', 'interest cr', 'stock profit'])) {
            return { category: 'Investment', confidence: 0.90, type: 'Income' };
        }

        // 2. Expense Categories
        if (this.containsAny(d, ['swiggy', 'zomato', 'mcdonald', 'starbucks', 'kfc', 'burger', 'pizza', 'dominos',
            'restaurant', 'cafe', 'dining', 'food', 'blinkit', 'zepto', 'instamart', 'bigbasket',
            'dmart', 'grocery', 'groceries', 'bakery', 'kitchen', 'eats', 'bar', 'pub'])) {
            return { category: 'Food', confidence: 0.95, type: 'Expense' };
        }

        if (this.containsAny(d, ['uber', 'ola', 'rapido', 'taxi', 'cab', 'auto', 'metro', 'fuel', 'petrol',
            'diesel', 'shell', 'hpcl', 'bpcl', 'iocl', 'toll', 'fastag', 'parking', 'bus ticket', 'gas station'])) {
            return { category: 'Transport', confidence: 0.95, type: 'Expense' };
        }

        if (this.containsAny(d, ['amazon', 'flipkart', 'myntra', 'zara', 'h&m', 'ajio', 'nykaa', 'retail',
            'mall', 'clothing', 'electronics', 'store', 'supermarket', 'shop', 'apparel', 'apple store'])) {
            return { category: 'Shopping', confidence: 0.92, type: 'Expense' };
        }

        if (this.containsAny(d, ['rent', 'landlord', 'society maintenance', 'housing', 'lease', 'apartment'])) {
            return { category: 'Rent', confidence: 0.95, type: 'Expense' };
        }

        if (this.containsAny(d, ['electricity', 'power bill', 'bescom', 'tata power', 'water bill', 'gas bill',
            'lpg', 'indane', 'hp gas', 'wifi', 'internet', 'broadband', 'airtel', 'jio', 'vi bill',
            'mobile recharge', 'recharge', 'dth'])) {
            return { category: 'Utilities', confidence: 0.94, type: 'Expense' };
        }

        if (this.containsAny(d, ['school', 'college', 'university', 'tuition', 'udemy', 'coursera', 'edx',
            'books', 'course', 'exam fee', 'academy', 'classes'])) {
            return { category: 'Education', confidence: 0.90, type: 'Expense' };
        }

        if (this.containsAny(d, ['hospital', 'clinic', 'pharmacy', 'apollo', 'medplus', '1mg', 'netmeds',
            'doctor', 'lab', 'dental', 'medical', 'chemist', 'healthcare', 'diagnostic'])) {
            return { category: 'Healthcare', confidence: 0.95, type: 'Expense' };
        }

        if (this.containsAny(d, ['netflix', 'spotify', 'prime video', 'hotstar', 'disney', 'cinema', 'pvr',
            'inox', 'movie', 'concert', 'gaming', 'steam', 'playstation', 'theatre', 'entertainment'])) {
            return { category: 'Entertainment', confidence: 0.94, type: 'Expense' };
        }

        if (this.containsAny(d, ['google one', 'icloud', 'apple.com/bill', 'chatgpt', 'openai', 'github',
            'linkedin premium', 'gym', 'cult.fit', 'fitness', 'membership', 'subscription'])) {
            return { category: 'Subscriptions', confidence: 0.92, type: 'Expense' };
        }

        if (this.containsAny(d, ['flight', 'indigo', 'air india', 'vistara', 'makemytrip', 'cleartrip',
            'easemytrip', 'goibibo', 'hotel', 'airbnb', 'booking.com', 'train', 'irctc', 'redbus',
            'travel', 'tour', 'resort'])) {
            return { category: 'Travel', confidence: 0.95, type: 'Expense' };
        }

        if (this.containsAny(d, ['zerodha', 'groww', 'upstox', 'mutual fund', 'sip', 'stocks', 'coin',
            'kuvera', 'smallcase', 'fixed deposit', 'fd', 'bonds', 'etf', 'securities'])) {
            return { category: 'Investment', confidence: 0.92, type: 'Expense' };
        }

        // Low confidence fallback
        return { category: 'Other', confidence: 0.40, type: currentType };
    },

    containsAny(text, keywords) {
        return keywords.some(k => text.includes(k));
    },

    detectPaymentMethod(description) {
        const d = (description || '').toLowerCase();
        if (d.includes('upi') || d.includes('vpa') || d.includes('@') || d.includes('gpay') || d.includes('phonepe') || d.includes('paytm')) {
            return 'UPI';
        }
        if (d.includes('credit card') || d.includes('cc ') || d.includes('visa') || d.includes('mastercard') || d.includes('amex')) {
            return 'Credit Card';
        }
        if (d.includes('debit card') || d.includes('dc ') || d.includes('pos') || d.includes('atm wdl')) {
            return 'Debit Card';
        }
        if (d.includes('neft') || d.includes('rtgs') || d.includes('imps') || d.includes('bank transfer') || d.includes('transfer') || d.includes('ach')) {
            return 'Bank Transfer';
        }
        if (d.includes('cash')) {
            return 'Cash';
        }
        return 'UPI';
    },

    normalizePaymentMethod(input) {
        const i = (input || '').toLowerCase().trim();
        if (i.includes('upi') || i.includes('gpay') || i.includes('phonepe')) return 'UPI';
        if (i.includes('credit')) return 'Credit Card';
        if (i.includes('debit')) return 'Debit Card';
        if (i.includes('cash')) return 'Cash';
        if (i.includes('bank') || i.includes('transfer') || i.includes('neft') || i.includes('imps')) return 'Bank Transfer';
        return 'Other';
    }
};

window.TransactionCategorizer = TransactionCategorizer;
