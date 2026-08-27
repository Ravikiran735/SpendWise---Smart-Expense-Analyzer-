/**
 * SpendWise — Duplicate Transaction Detector
 * Deterministic SHA-256 fingerprinting matching Android algorithm
 */

const DuplicateDetector = {
    /**
     * Compute SHA-256 fingerprint hash for a transaction
     */
    async computeFingerprint(dateObj, amount, description, type, paymentMethod) {
        const d = dateObj instanceof Date ? dateObj : new Date(dateObj);
        const yyyy = d.getFullYear();
        const mm = String(d.getMonth() + 1).padStart(2, '0');
        const dd = String(d.getDate()).padStart(2, '0');
        const dateStr = `${yyyy}-${mm}-${dd}`;

        const amtStr = Number(amount || 0).toFixed(2);
        const cleanDesc = (description || '').toLowerCase().replace(/[^a-z0-9]/g, '');
        const typeStr = (type || 'Expense').toLowerCase();
        const methodStr = (paymentMethod || 'UPI').toLowerCase();

        const rawKey = `${dateStr}|${amtStr}|${cleanDesc}|${typeStr}|${methodStr}`;

        // Compute SHA-256 using Web Crypto API
        const msgBuffer = new TextEncoder().encode(rawKey);
        const hashBuffer = await crypto.subtle.digest('SHA-256', msgBuffer);
        const hashArray = Array.from(new Uint8Array(hashBuffer));
        const hashHex = hashArray.map(b => b.toString(16).padStart(2, '0')).join('');
        return hashHex;
    },

    /**
     * Build existing transaction fingerprint set from Firestore state
     */
    async buildExistingFingerprints(expenses, incomes) {
        const set = new Set();
        for (const exp of expenses || []) {
            const fp = await this.computeFingerprint(exp.date, exp.amount, exp.description, 'Expense', exp.paymentMethod || 'UPI');
            set.add(fp);
        }
        for (const inc of incomes || []) {
            const fp = await this.computeFingerprint(inc.date, inc.amount, inc.description, 'Income', inc.paymentMethod || 'Bank Transfer');
            set.add(fp);
        }
        return set;
    }
};

window.DuplicateDetector = DuplicateDetector;
