#!/usr/bin/env bash

PATH="$PATH:$HOME/software/pulumi"
export PATH

# If not running interactively, don't do anything
[[ $- != *i* ]] && return

GPG_TTY="$(tty)"
export GPG_TTY

alias cljs="clj -Sdeps '{:deps{org.clojure/clojurescript{:mvn/version\"1.10.896\"}}}' -M -m cljs.main -re node"

alias ls='ls --color=auto'
# PS1='[\u@\h \W]\$ '
git_branch() {
  GIT_STATUS="$(git status --porcelain 2> /dev/null)"
  (( $? == 0 )) || return
  [[ -z $GIT_STATUS ]] || return
  echo -n "$(git branch --show-current 2> /dev/null) "
}

git_branch_MOD() {
  GIT_STATUS="$(git status --porcelain 2> /dev/null)"
  (( $? == 0 )) || return
  [[ -z $GIT_STATUS ]] && return
  echo -n "$(git branch --show-current 2> /dev/null) "
}
# PS1='$(git_branch)\W'
C_GREEN='\[\e[1;32m\]'
C_RED='\[\e[1;31m\]'
C_BLUE='\[\e[1;34m\]'
C_WIPE='\[\e[0m\]'
PS1="\n[\${PIPESTATUS[@]} $C_BLUE\$(git_branch)$C_RED\$(git_branch_MOD)$C_WIPE$C_GREEN\w$C_WIPE \t]\n"
# PS1='[\u@\h \W]\$ '
