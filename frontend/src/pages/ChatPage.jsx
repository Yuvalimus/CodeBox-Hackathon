import React from 'react';
import ChatPanel from '../components/ChatPanel.jsx';
export default function ChatPage({ profile, match, navigate, onUnmatch }) {
  return <main className="home-main chat-page"><a href="/home" onClick={navigate}>← Home</a>{profile ? <ChatPanel profile={profile} initialChatId={match?.chatId} onUnmatch={onUnmatch} /> : <p><a href="/login" onClick={navigate}>Log in to open your chats</a></p>}</main>;
}
