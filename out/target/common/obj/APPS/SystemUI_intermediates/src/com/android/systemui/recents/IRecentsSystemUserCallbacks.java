/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: frameworks/base/packages/SystemUI/src/com/android/systemui/recents/IRecentsSystemUserCallbacks.aidl
 */
package com.android.systemui.recents;
/**
 * Due to the fact that RecentsActivity is per-user, we need to establish an
 * interface (this) for the non-system user to register itself for callbacks and to
 * callback to the system user to update internal state.
 */
public interface IRecentsSystemUserCallbacks extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements com.android.systemui.recents.IRecentsSystemUserCallbacks
{
private static final java.lang.String DESCRIPTOR = "com.android.systemui.recents.IRecentsSystemUserCallbacks";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an com.android.systemui.recents.IRecentsSystemUserCallbacks interface,
 * generating a proxy if needed.
 */
public static com.android.systemui.recents.IRecentsSystemUserCallbacks asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof com.android.systemui.recents.IRecentsSystemUserCallbacks))) {
return ((com.android.systemui.recents.IRecentsSystemUserCallbacks)iin);
}
return new com.android.systemui.recents.IRecentsSystemUserCallbacks.Stub.Proxy(obj);
}
@Override public android.os.IBinder asBinder()
{
return this;
}
@Override public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException
{
switch (code)
{
case INTERFACE_TRANSACTION:
{
reply.writeString(DESCRIPTOR);
return true;
}
case TRANSACTION_registerNonSystemUserCallbacks:
{
data.enforceInterface(DESCRIPTOR);
android.os.IBinder _arg0;
_arg0 = data.readStrongBinder();
int _arg1;
_arg1 = data.readInt();
this.registerNonSystemUserCallbacks(_arg0, _arg1);
return true;
}
case TRANSACTION_updateRecentsVisibility:
{
data.enforceInterface(DESCRIPTOR);
boolean _arg0;
_arg0 = (0!=data.readInt());
this.updateRecentsVisibility(_arg0);
return true;
}
case TRANSACTION_startScreenPinning:
{
data.enforceInterface(DESCRIPTOR);
int _arg0;
_arg0 = data.readInt();
this.startScreenPinning(_arg0);
return true;
}
case TRANSACTION_sendRecentsDrawnEvent:
{
data.enforceInterface(DESCRIPTOR);
this.sendRecentsDrawnEvent();
return true;
}
case TRANSACTION_sendDockingTopTaskEvent:
{
data.enforceInterface(DESCRIPTOR);
int _arg0;
_arg0 = data.readInt();
android.graphics.Rect _arg1;
if ((0!=data.readInt())) {
_arg1 = android.graphics.Rect.CREATOR.createFromParcel(data);
}
else {
_arg1 = null;
}
this.sendDockingTopTaskEvent(_arg0, _arg1);
return true;
}
case TRANSACTION_sendLaunchRecentsEvent:
{
data.enforceInterface(DESCRIPTOR);
this.sendLaunchRecentsEvent();
return true;
}
case TRANSACTION_setWaitingForTransitionStartEvent:
{
data.enforceInterface(DESCRIPTOR);
boolean _arg0;
_arg0 = (0!=data.readInt());
this.setWaitingForTransitionStartEvent(_arg0);
return true;
}
}
return super.onTransact(code, data, reply, flags);
}
private static class Proxy implements com.android.systemui.recents.IRecentsSystemUserCallbacks
{
private android.os.IBinder mRemote;
Proxy(android.os.IBinder remote)
{
mRemote = remote;
}
@Override public android.os.IBinder asBinder()
{
return mRemote;
}
public java.lang.String getInterfaceDescriptor()
{
return DESCRIPTOR;
}
@Override public void registerNonSystemUserCallbacks(android.os.IBinder nonSystemUserCallbacks, int userId) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeStrongBinder(nonSystemUserCallbacks);
_data.writeInt(userId);
mRemote.transact(Stub.TRANSACTION_registerNonSystemUserCallbacks, _data, null, android.os.IBinder.FLAG_ONEWAY);
}
finally {
_data.recycle();
}
}
@Override public void updateRecentsVisibility(boolean visible) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(((visible)?(1):(0)));
mRemote.transact(Stub.TRANSACTION_updateRecentsVisibility, _data, null, android.os.IBinder.FLAG_ONEWAY);
}
finally {
_data.recycle();
}
}
@Override public void startScreenPinning(int taskId) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(taskId);
mRemote.transact(Stub.TRANSACTION_startScreenPinning, _data, null, android.os.IBinder.FLAG_ONEWAY);
}
finally {
_data.recycle();
}
}
@Override public void sendRecentsDrawnEvent() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_sendRecentsDrawnEvent, _data, null, android.os.IBinder.FLAG_ONEWAY);
}
finally {
_data.recycle();
}
}
@Override public void sendDockingTopTaskEvent(int dragMode, android.graphics.Rect initialRect) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(dragMode);
if ((initialRect!=null)) {
_data.writeInt(1);
initialRect.writeToParcel(_data, 0);
}
else {
_data.writeInt(0);
}
mRemote.transact(Stub.TRANSACTION_sendDockingTopTaskEvent, _data, null, android.os.IBinder.FLAG_ONEWAY);
}
finally {
_data.recycle();
}
}
@Override public void sendLaunchRecentsEvent() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_sendLaunchRecentsEvent, _data, null, android.os.IBinder.FLAG_ONEWAY);
}
finally {
_data.recycle();
}
}
@Override public void setWaitingForTransitionStartEvent(boolean waitingForTransitionStart) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(((waitingForTransitionStart)?(1):(0)));
mRemote.transact(Stub.TRANSACTION_setWaitingForTransitionStartEvent, _data, null, android.os.IBinder.FLAG_ONEWAY);
}
finally {
_data.recycle();
}
}
}
static final int TRANSACTION_registerNonSystemUserCallbacks = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
static final int TRANSACTION_updateRecentsVisibility = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
static final int TRANSACTION_startScreenPinning = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
static final int TRANSACTION_sendRecentsDrawnEvent = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
static final int TRANSACTION_sendDockingTopTaskEvent = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
static final int TRANSACTION_sendLaunchRecentsEvent = (android.os.IBinder.FIRST_CALL_TRANSACTION + 5);
static final int TRANSACTION_setWaitingForTransitionStartEvent = (android.os.IBinder.FIRST_CALL_TRANSACTION + 6);
}
public void registerNonSystemUserCallbacks(android.os.IBinder nonSystemUserCallbacks, int userId) throws android.os.RemoteException;
public void updateRecentsVisibility(boolean visible) throws android.os.RemoteException;
public void startScreenPinning(int taskId) throws android.os.RemoteException;
public void sendRecentsDrawnEvent() throws android.os.RemoteException;
public void sendDockingTopTaskEvent(int dragMode, android.graphics.Rect initialRect) throws android.os.RemoteException;
public void sendLaunchRecentsEvent() throws android.os.RemoteException;
public void setWaitingForTransitionStartEvent(boolean waitingForTransitionStart) throws android.os.RemoteException;
}
