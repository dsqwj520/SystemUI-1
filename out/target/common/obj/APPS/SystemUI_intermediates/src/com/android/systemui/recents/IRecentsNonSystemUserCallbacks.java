/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: frameworks/base/packages/SystemUI/src/com/android/systemui/recents/IRecentsNonSystemUserCallbacks.aidl
 */
package com.android.systemui.recents;
/**
 * Due to the fact that RecentsActivity is per-user, we need to establish an
 * interface (this) for the system user to callback to the secondary users in
 * response to UI events coming in from the system user's SystemUI.
 */
public interface IRecentsNonSystemUserCallbacks extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements com.android.systemui.recents.IRecentsNonSystemUserCallbacks
{
private static final java.lang.String DESCRIPTOR = "com.android.systemui.recents.IRecentsNonSystemUserCallbacks";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an com.android.systemui.recents.IRecentsNonSystemUserCallbacks interface,
 * generating a proxy if needed.
 */
public static com.android.systemui.recents.IRecentsNonSystemUserCallbacks asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof com.android.systemui.recents.IRecentsNonSystemUserCallbacks))) {
return ((com.android.systemui.recents.IRecentsNonSystemUserCallbacks)iin);
}
return new com.android.systemui.recents.IRecentsNonSystemUserCallbacks.Stub.Proxy(obj);
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
case TRANSACTION_preloadRecents:
{
data.enforceInterface(DESCRIPTOR);
this.preloadRecents();
return true;
}
case TRANSACTION_cancelPreloadingRecents:
{
data.enforceInterface(DESCRIPTOR);
this.cancelPreloadingRecents();
return true;
}
case TRANSACTION_showRecents:
{
data.enforceInterface(DESCRIPTOR);
boolean _arg0;
_arg0 = (0!=data.readInt());
boolean _arg1;
_arg1 = (0!=data.readInt());
boolean _arg2;
_arg2 = (0!=data.readInt());
boolean _arg3;
_arg3 = (0!=data.readInt());
boolean _arg4;
_arg4 = (0!=data.readInt());
int _arg5;
_arg5 = data.readInt();
this.showRecents(_arg0, _arg1, _arg2, _arg3, _arg4, _arg5);
return true;
}
case TRANSACTION_hideRecents:
{
data.enforceInterface(DESCRIPTOR);
boolean _arg0;
_arg0 = (0!=data.readInt());
boolean _arg1;
_arg1 = (0!=data.readInt());
this.hideRecents(_arg0, _arg1);
return true;
}
case TRANSACTION_toggleRecents:
{
data.enforceInterface(DESCRIPTOR);
int _arg0;
_arg0 = data.readInt();
this.toggleRecents(_arg0);
return true;
}
case TRANSACTION_onConfigurationChanged:
{
data.enforceInterface(DESCRIPTOR);
this.onConfigurationChanged();
return true;
}
case TRANSACTION_dockTopTask:
{
data.enforceInterface(DESCRIPTOR);
int _arg0;
_arg0 = data.readInt();
int _arg1;
_arg1 = data.readInt();
int _arg2;
_arg2 = data.readInt();
android.graphics.Rect _arg3;
if ((0!=data.readInt())) {
_arg3 = android.graphics.Rect.CREATOR.createFromParcel(data);
}
else {
_arg3 = null;
}
this.dockTopTask(_arg0, _arg1, _arg2, _arg3);
return true;
}
case TRANSACTION_onDraggingInRecents:
{
data.enforceInterface(DESCRIPTOR);
float _arg0;
_arg0 = data.readFloat();
this.onDraggingInRecents(_arg0);
return true;
}
case TRANSACTION_onDraggingInRecentsEnded:
{
data.enforceInterface(DESCRIPTOR);
float _arg0;
_arg0 = data.readFloat();
this.onDraggingInRecentsEnded(_arg0);
return true;
}
case TRANSACTION_showCurrentUserToast:
{
data.enforceInterface(DESCRIPTOR);
int _arg0;
_arg0 = data.readInt();
int _arg1;
_arg1 = data.readInt();
this.showCurrentUserToast(_arg0, _arg1);
return true;
}
}
return super.onTransact(code, data, reply, flags);
}
private static class Proxy implements com.android.systemui.recents.IRecentsNonSystemUserCallbacks
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
@Override public void preloadRecents() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_preloadRecents, _data, null, android.os.IBinder.FLAG_ONEWAY);
}
finally {
_data.recycle();
}
}
@Override public void cancelPreloadingRecents() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_cancelPreloadingRecents, _data, null, android.os.IBinder.FLAG_ONEWAY);
}
finally {
_data.recycle();
}
}
@Override public void showRecents(boolean triggeredFromAltTab, boolean draggingInRecents, boolean animate, boolean reloadTasks, boolean fromHome, int recentsGrowTarget) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(((triggeredFromAltTab)?(1):(0)));
_data.writeInt(((draggingInRecents)?(1):(0)));
_data.writeInt(((animate)?(1):(0)));
_data.writeInt(((reloadTasks)?(1):(0)));
_data.writeInt(((fromHome)?(1):(0)));
_data.writeInt(recentsGrowTarget);
mRemote.transact(Stub.TRANSACTION_showRecents, _data, null, android.os.IBinder.FLAG_ONEWAY);
}
finally {
_data.recycle();
}
}
@Override public void hideRecents(boolean triggeredFromAltTab, boolean triggeredFromHomeKey) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(((triggeredFromAltTab)?(1):(0)));
_data.writeInt(((triggeredFromHomeKey)?(1):(0)));
mRemote.transact(Stub.TRANSACTION_hideRecents, _data, null, android.os.IBinder.FLAG_ONEWAY);
}
finally {
_data.recycle();
}
}
@Override public void toggleRecents(int recentsGrowTarget) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(recentsGrowTarget);
mRemote.transact(Stub.TRANSACTION_toggleRecents, _data, null, android.os.IBinder.FLAG_ONEWAY);
}
finally {
_data.recycle();
}
}
@Override public void onConfigurationChanged() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_onConfigurationChanged, _data, null, android.os.IBinder.FLAG_ONEWAY);
}
finally {
_data.recycle();
}
}
@Override public void dockTopTask(int topTaskId, int dragMode, int stackCreateMode, android.graphics.Rect initialBounds) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(topTaskId);
_data.writeInt(dragMode);
_data.writeInt(stackCreateMode);
if ((initialBounds!=null)) {
_data.writeInt(1);
initialBounds.writeToParcel(_data, 0);
}
else {
_data.writeInt(0);
}
mRemote.transact(Stub.TRANSACTION_dockTopTask, _data, null, android.os.IBinder.FLAG_ONEWAY);
}
finally {
_data.recycle();
}
}
@Override public void onDraggingInRecents(float distanceFromTop) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeFloat(distanceFromTop);
mRemote.transact(Stub.TRANSACTION_onDraggingInRecents, _data, null, android.os.IBinder.FLAG_ONEWAY);
}
finally {
_data.recycle();
}
}
@Override public void onDraggingInRecentsEnded(float velocity) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeFloat(velocity);
mRemote.transact(Stub.TRANSACTION_onDraggingInRecentsEnded, _data, null, android.os.IBinder.FLAG_ONEWAY);
}
finally {
_data.recycle();
}
}
@Override public void showCurrentUserToast(int msgResId, int msgLength) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(msgResId);
_data.writeInt(msgLength);
mRemote.transact(Stub.TRANSACTION_showCurrentUserToast, _data, null, android.os.IBinder.FLAG_ONEWAY);
}
finally {
_data.recycle();
}
}
}
static final int TRANSACTION_preloadRecents = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
static final int TRANSACTION_cancelPreloadingRecents = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
static final int TRANSACTION_showRecents = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
static final int TRANSACTION_hideRecents = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
static final int TRANSACTION_toggleRecents = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
static final int TRANSACTION_onConfigurationChanged = (android.os.IBinder.FIRST_CALL_TRANSACTION + 5);
static final int TRANSACTION_dockTopTask = (android.os.IBinder.FIRST_CALL_TRANSACTION + 6);
static final int TRANSACTION_onDraggingInRecents = (android.os.IBinder.FIRST_CALL_TRANSACTION + 7);
static final int TRANSACTION_onDraggingInRecentsEnded = (android.os.IBinder.FIRST_CALL_TRANSACTION + 8);
static final int TRANSACTION_showCurrentUserToast = (android.os.IBinder.FIRST_CALL_TRANSACTION + 9);
}
public void preloadRecents() throws android.os.RemoteException;
public void cancelPreloadingRecents() throws android.os.RemoteException;
public void showRecents(boolean triggeredFromAltTab, boolean draggingInRecents, boolean animate, boolean reloadTasks, boolean fromHome, int recentsGrowTarget) throws android.os.RemoteException;
public void hideRecents(boolean triggeredFromAltTab, boolean triggeredFromHomeKey) throws android.os.RemoteException;
public void toggleRecents(int recentsGrowTarget) throws android.os.RemoteException;
public void onConfigurationChanged() throws android.os.RemoteException;
public void dockTopTask(int topTaskId, int dragMode, int stackCreateMode, android.graphics.Rect initialBounds) throws android.os.RemoteException;
public void onDraggingInRecents(float distanceFromTop) throws android.os.RemoteException;
public void onDraggingInRecentsEnded(float velocity) throws android.os.RemoteException;
public void showCurrentUserToast(int msgResId, int msgLength) throws android.os.RemoteException;
}
