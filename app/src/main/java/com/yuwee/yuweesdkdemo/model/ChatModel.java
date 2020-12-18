package com.yuwee.yuweesdkdemo.model;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONObject;

public class ChatModel implements Parcelable {
    public ViewType viewType;
    public String name, message, messageTime, roomId;
    public String messageIdentifier, messageId;
    public CallData callData = new CallData(Parcel.obtain());
    public FileData fileData;
    public long lastMessageTime;
    public boolean isForwarded;
    public String senderId, senderName, senderEmail, senderImage, messageType;
    public String quoteJson;
    public long unreadCount;

    public static class FileData implements Parcelable {
        public String fileName, fileUrl, fileId, fileKey;

        public FileData(Parcel in) {
            fileName = in.readString();
            fileUrl = in.readString();
            fileId = in.readString();
            fileKey = in.readString();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(fileName);
            dest.writeString(fileUrl);
            dest.writeString(fileId);
            dest.writeString(fileKey);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        public static final Creator<FileData> CREATOR = new Creator<FileData>() {
            @Override
            public FileData createFromParcel(Parcel in) {
                return new FileData(in);
            }

            @Override
            public FileData[] newArray(int size) {
                return new FileData[size];
            }
        };
    }

    public static class CallData implements Parcelable {
        public String callerName, calleeName;
        public String callerId;

        public CallData(Parcel in) {
            callerName = in.readString();
            calleeName = in.readString();
            callerId = in.readString();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(callerName);
            dest.writeString(calleeName);
            dest.writeString(callerId);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        public static final Creator<CallData> CREATOR = new Creator<CallData>() {
            @Override
            public CallData createFromParcel(Parcel in) {
                return new CallData(in);
            }

            @Override
            public CallData[] newArray(int size) {
                return new CallData[size];
            }
        };
    }

    public ChatModel(Parcel in) {
        name = in.readString();
        message = in.readString();
        messageTime = in.readString();
        roomId = in.readString();
        messageIdentifier = in.readString();
        messageId = in.readString();
        callData = in.readParcelable(CallData.class.getClassLoader());
        fileData = in.readParcelable(FileData.class.getClassLoader());
        lastMessageTime = in.readLong();
        isForwarded = in.readByte() != 0;
        senderId = in.readString();
        senderName = in.readString();
        senderEmail = in.readString();
        senderImage = in.readString();
        messageType = in.readString();
        quoteJson = in.readString();
        unreadCount = in.readLong();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(message);
        dest.writeString(messageTime);
        dest.writeString(roomId);
        dest.writeString(messageIdentifier);
        dest.writeString(messageId);
        dest.writeParcelable(callData, flags);
        dest.writeParcelable(fileData, flags);
        dest.writeLong(lastMessageTime);
        dest.writeByte((byte) (isForwarded ? 1 : 0));
        dest.writeString(senderId);
        dest.writeString(senderName);
        dest.writeString(senderEmail);
        dest.writeString(senderImage);
        dest.writeString(messageType);
        dest.writeString(quoteJson);
        dest.writeLong(unreadCount);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<ChatModel> CREATOR = new Creator<ChatModel>() {
        @Override
        public ChatModel createFromParcel(Parcel in) {
            return new ChatModel(in);
        }

        @Override
        public ChatModel[] newArray(int size) {
            return new ChatModel[size];
        }
    };
}
