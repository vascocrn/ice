package org.jbei.ice.lib.dto.comment;

import java.util.Date;

import org.jbei.ice.lib.account.AccountTransfer;
import org.jbei.ice.lib.dao.IDataTransferModel;

/**
 * DTO for {@link org.jbei.ice.lib.models.Comment}. Comments are tied to specific entries
 * and the entryId field is used to uniquely identify the entry this comment is tied to
 *
 * @author Hector Plahar
 */
public class UserComment implements IDataTransferModel {

    private long id;
    private AccountTransfer accountTransfer;
    private String message;
    private Date commentDate;
    private long entryId;

    public UserComment() {
    }

    public UserComment(String message) {
        this.message = message;
    }

    public AccountTransfer getAccountTransfer() {
        return accountTransfer;
    }

    public void setAccountTransfer(AccountTransfer accountTransfer) {
        this.accountTransfer = accountTransfer;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Date getCommentDate() {
        return commentDate;
    }

    public void setCommentDate(Date commentDate) {
        this.commentDate = commentDate;
    }

    public long getEntryId() {
        return entryId;
    }

    public void setEntryId(long entryId) {
        this.entryId = entryId;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }
}
