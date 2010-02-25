package org.jbei.ice.lib.managers;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.jbei.ice.lib.logging.Logger;
import org.jbei.ice.lib.models.Account;
import org.jbei.ice.lib.models.AccountPreferences;
import org.jbei.ice.lib.models.Moderator;
import org.jbei.ice.lib.utils.JbeirSettings;
import org.jbei.ice.lib.utils.Utils;

public class AccountManager extends Manager {
    public static AccountPreferences getAccountPreferences(int id) throws ManagerException {
        AccountPreferences accountPreferences = null;
        try {
            Query query = HibernateHelper.getSession().createQuery(
                    "from AccountPreferences where id = :id");
            query.setEntity("id", id);

            accountPreferences = (AccountPreferences) query.uniqueResult();
        } catch (Exception e) {
            String msg = "Could not get AccountPreferences by id";
            Logger.error(msg);
            throw new ManagerException(msg);
        }

        return accountPreferences;
    }

    public static AccountPreferences getAccountPreferences(Account account) throws ManagerException {
        AccountPreferences accountPreferences = null;
        try {
            Query query = HibernateHelper.getSession().createQuery(
                    "from AccountPreferences where account = :account");
            query.setParameter("account", account);

            accountPreferences = (AccountPreferences) query.uniqueResult();
        } catch (Exception e) {
            throw new ManagerException("Could not get AccountPreferences by account");
        }

        return accountPreferences;
    }

    public static AccountPreferences save(AccountPreferences accountPreferences)
            throws ManagerException {
        AccountPreferences result;
        try {
            result = (AccountPreferences) dbSave(accountPreferences);
        } catch (Exception e) {
            throw new ManagerException("Could not create AccountPreferences in db");
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public static Account getAccountByAuthToken(String authToken) {
        Account account = null;
        String queryString = "select data from SessionData sessionData where sessionData.sessionKey = :sessionKey";
        try {
            Query query = getSession().createQuery(queryString);
            query.setString("sessionKey", authToken);
            HashMap<String, Object> sessionData = (HashMap<String, Object>) query.uniqueResult();
            if (sessionData != null) {
                account = get((Integer) sessionData.get("accountId"));
            }

        } catch (HibernateException e) {
            Logger.info("Could not retrieve account by sessionKey");
        } catch (ManagerException e) {
            Logger.info("Could not retrieve account by sessionKey");
            e.printStackTrace();
        }
        return account;
    }

    public static Account get(int id) throws ManagerException {
        Account account = null;
        try {
            Query query = HibernateHelper.getSession().createQuery("from Account where id = :id");
            query.setParameter("id", id);

            account = (Account) query.uniqueResult();
        } catch (HibernateException e) {
            Logger.warn("Couldn't retrieve Account by id");
            throw new ManagerException("Couldn't retrieve Account by id: " + String.valueOf(id), e);
        }

        return account;
    }

    @SuppressWarnings("unchecked")
    public static Set<Account> getAll() throws ManagerException {
        LinkedHashSet<Account> accounts = new LinkedHashSet<Account>();
        try {
            String queryString = "from Account";
            Query query = getSession().createQuery(queryString);
            accounts.addAll(query.list());
        } catch (HibernateException e) {
            String msg = "Could not retrieve all accounts " + e.toString();
            Logger.warn(msg);
            throw new ManagerException(msg);
        }

        return accounts;
    }

    @SuppressWarnings("unchecked")
    public static Set<Account> getAllByFirstName() throws ManagerException {
        LinkedHashSet<Account> accounts = new LinkedHashSet<Account>();
        try {
            String queryString = "from Account order by firstName";
            Query query = getSession().createQuery(queryString);
            accounts.addAll(query.list());
        } catch (HibernateException e) {
            String msg = "Could not retrieve all accounts " + e.toString();
            Logger.warn(msg);
            throw new ManagerException(msg);
        }

        return accounts;
    }

    public static Account getByEmail(String email) throws ManagerException {
        Account account = null;
        try {
            Query query = HibernateHelper.getSession().createQuery(
                    "from Account where email = :email");
            query.setParameter("email", email);
            account = (Account) query.uniqueResult();
        } catch (HibernateException e) {
            Logger.warn("Couldn't retrieve Account by email");
            throw new ManagerException("Couldn't retrieve Account by email: " + email);
        }

        return account;
    }

    public static Boolean isModerator(Account account) {
        Boolean result = false;
        try {
            String queryString = "from Moderator moderator where moderator.account = :account";
            Query query = HibernateHelper.getSession().createQuery(queryString);
            query.setParameter("account", account);
            Moderator moderator = (Moderator) query.uniqueResult();
            if (moderator != null) {
                result = true;
            }
        } catch (HibernateException e) {
            Logger.error("Could not determine moderator for account: " + account.getEmail());
        }
        return result;
    }

    public static Account save(Account account) throws ManagerException {
        try {
            Account result = (Account) dbSave(account);

            return result;
        } catch (Exception e) {
            String msg = "Could not save account " + account.getEmail();

            Logger.error(msg);

            throw new ManagerException(msg);
        }
    }

    public static String encryptPassword(String password) {
        String md5key = Utils.encryptMD5(JbeirSettings.getSetting("SECRET_KEY") + password);

        return md5key;
    }
}
