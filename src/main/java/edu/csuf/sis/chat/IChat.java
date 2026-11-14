package edu.csuf.sis.chat;

/**
 * Chatbot Assistant component: answers course and policy questions from catalog data.
 * It has no access to grades or other personal records.
 */
public interface IChat {

    ChatReply ask(String question);
}
