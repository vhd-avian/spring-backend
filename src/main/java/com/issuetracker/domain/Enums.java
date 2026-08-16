package com.issuetracker.domain;

public class Enums {
  public enum GlobalRole { admin, user }
  public enum ProjectRole { admin, lead, member, viewer }
  public enum IssueType { task, bug, story }
  public enum IssueStatus { backlog, todo, in_progress, in_review, done }
  public enum Priority { lowest, low, medium, high, highest }
}
