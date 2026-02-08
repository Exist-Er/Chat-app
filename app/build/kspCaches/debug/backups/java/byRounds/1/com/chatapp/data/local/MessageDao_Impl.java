package com.chatapp.data.local;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.chatapp.data.model.Message;
import com.chatapp.data.model.MessageStatus;
import com.chatapp.data.model.MessageType;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class MessageDao_Impl implements MessageDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<Message> __insertionAdapterOfMessage;

  private final DataConverters __dataConverters = new DataConverters();

  private final SharedSQLiteStatement __preparedStmtOfUpdateMessageStatus;

  private final SharedSQLiteStatement __preparedStmtOfDeleteChatHistory;

  public MessageDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfMessage = new EntityInsertionAdapter<Message>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `messages` (`messageId`,`chatId`,`senderId`,`recipientId`,`content`,`timestamp`,`status`,`type`,`sequenceNumber`) VALUES (?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Message entity) {
        statement.bindString(1, entity.getMessageId());
        statement.bindString(2, entity.getChatId());
        statement.bindString(3, entity.getSenderId());
        statement.bindString(4, entity.getRecipientId());
        statement.bindString(5, entity.getContent());
        statement.bindLong(6, entity.getTimestamp());
        final String _tmp = __dataConverters.fromStatus(entity.getStatus());
        statement.bindString(7, _tmp);
        final String _tmp_1 = __dataConverters.fromType(entity.getType());
        statement.bindString(8, _tmp_1);
        statement.bindLong(9, entity.getSequenceNumber());
      }
    };
    this.__preparedStmtOfUpdateMessageStatus = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE messages SET status = ? WHERE messageId = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteChatHistory = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM messages WHERE chatId = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertMessage(final Message message, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfMessage.insert(message);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertMessages(final List<Message> messages,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfMessage.insert(messages);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateMessageStatus(final String messageId, final String status,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateMessageStatus.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, status);
        _argIndex = 2;
        _stmt.bindString(_argIndex, messageId);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfUpdateMessageStatus.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteChatHistory(final String chatId,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteChatHistory.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, chatId);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteChatHistory.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<Message>> getMessagesForChat(final String chatId) {
    final String _sql = "SELECT * FROM messages WHERE chatId = ? ORDER BY timestamp ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, chatId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"messages"}, new Callable<List<Message>>() {
      @Override
      @NonNull
      public List<Message> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfMessageId = CursorUtil.getColumnIndexOrThrow(_cursor, "messageId");
          final int _cursorIndexOfChatId = CursorUtil.getColumnIndexOrThrow(_cursor, "chatId");
          final int _cursorIndexOfSenderId = CursorUtil.getColumnIndexOrThrow(_cursor, "senderId");
          final int _cursorIndexOfRecipientId = CursorUtil.getColumnIndexOrThrow(_cursor, "recipientId");
          final int _cursorIndexOfContent = CursorUtil.getColumnIndexOrThrow(_cursor, "content");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfSequenceNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "sequenceNumber");
          final List<Message> _result = new ArrayList<Message>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Message _item;
            final String _tmpMessageId;
            _tmpMessageId = _cursor.getString(_cursorIndexOfMessageId);
            final String _tmpChatId;
            _tmpChatId = _cursor.getString(_cursorIndexOfChatId);
            final String _tmpSenderId;
            _tmpSenderId = _cursor.getString(_cursorIndexOfSenderId);
            final String _tmpRecipientId;
            _tmpRecipientId = _cursor.getString(_cursorIndexOfRecipientId);
            final String _tmpContent;
            _tmpContent = _cursor.getString(_cursorIndexOfContent);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final MessageStatus _tmpStatus;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfStatus);
            _tmpStatus = __dataConverters.toStatus(_tmp);
            final MessageType _tmpType;
            final String _tmp_1;
            _tmp_1 = _cursor.getString(_cursorIndexOfType);
            _tmpType = __dataConverters.toType(_tmp_1);
            final long _tmpSequenceNumber;
            _tmpSequenceNumber = _cursor.getLong(_cursorIndexOfSequenceNumber);
            _item = new Message(_tmpMessageId,_tmpChatId,_tmpSenderId,_tmpRecipientId,_tmpContent,_tmpTimestamp,_tmpStatus,_tmpType,_tmpSequenceNumber);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
