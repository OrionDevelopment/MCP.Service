using System;
using System.Collections.Generic;
using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace Data.Core.Models.Mapping.Proposals
{

    /// <summary>
    /// Represents a single comment made on a proposal.
    /// </summary>
    public class ProposalComment
    {
        /// <summary>
        /// The id of the comment.
        /// </summary>
        [Key]
        [DatabaseGenerated(DatabaseGeneratedOption.Identity)]
        public Guid Id { get; set; }

        /// <summary>
        /// The id of the user who created the comment.
        /// </summary>
        [Required]
        public Guid CreatedBy { get; set; }

        /// <summary>
        /// The moment the comment was created.
        /// </summary>
        [Required]
        public DateTime CreatedOn { get; set; }

        /// <summary>
        /// The content of the comment.
        /// </summary>
        [Required]
        public string Content { get; set; }

        /// <summary>
        /// The reactions applied to the comment.
        /// </summary>
        public List<ProposalCommentReaction> Reactions { get; set; }

        /// <summary>
        /// Indicates if the comment has been edited.
        /// </summary>
        public bool HasBeenEdited { get; set; }

        /// <summary>
        /// Indicates if the comment has been deleted.
        /// </summary>
        public bool IsDeleted { get; set; }

        /// <summary>
        /// The user who deleted the comment, can be the creating user or any user with enough rights.
        /// </summary>
        public Guid DeletedBy { get; set; }
    }
}
